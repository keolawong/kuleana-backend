package com.kuleanasystems.auth.controller;

import com.kuleanasystems.auth.dto.LoginRequest;
import com.kuleanasystems.auth.dto.RegistrationRequest;
import com.kuleanasystems.auth.entity.Role;
import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.entity.UserApplication;
import com.kuleanasystems.auth.entity.UserRole;
import com.kuleanasystems.auth.entity.UserSecurityQuestion;
import com.kuleanasystems.auth.entity.Organization;
import com.kuleanasystems.auth.entity.UserOrganization;
import com.kuleanasystems.auth.repository.RolePageRepository;
import com.kuleanasystems.auth.repository.RoleRepository;
import com.kuleanasystems.auth.repository.UserApplicationRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import com.kuleanasystems.auth.repository.UserRoleRepository;
import com.kuleanasystems.auth.repository.UserSecurityQuestionRepository;
import com.kuleanasystems.auth.repository.OrganizationRepository;
import com.kuleanasystems.auth.repository.UserOrganizationRepository;
import com.kuleanasystems.auth.service.AdminScopeService;
import com.kuleanasystems.auth.service.AuthService;
import com.kuleanasystems.auth.service.PasswordValidator;
import com.kuleanasystems.auth.service.TokenBlacklistService;
import com.kuleanasystems.auth.service.TokenRevocationService;
import com.kuleanasystems.auth.service.UserAuthCacheService;
import com.kuleanasystems.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired(required = false)
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSecurityQuestionRepository userSecurityQuestionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAuthCacheService userAuthCacheService;

    @Autowired(required = false)
    private UserRoleRepository userRoleRepository;

    @Autowired(required = false)
    private RoleRepository roleRepository;

    @Autowired(required = false)
    private UserApplicationRepository userApplicationRepository;

    @Autowired(required = false)
    private RolePageRepository rolePageRepository;

    @Autowired(required = false)
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired(required = false)
    private OrganizationRepository organizationRepository;

    @Autowired(required = false)
    private AdminScopeService adminScopeService;

    @Autowired(required = false)
    private com.kuleanasystems.auth.repository.AppRegistrationConfigRepository appRegistrationConfigRepository;

    @Autowired
    private com.kuleanasystems.auth.service.CaptchaVerificationService captchaVerificationService;

    @org.springframework.beans.factory.annotation.Value("${security.captcha.turnstile.site-key:1x00000000000000000000AA}")
    private String turnstileSiteKey;

    @GetMapping("/registration-config")
    public ResponseEntity<?> getRegistrationConfig(
            @RequestParam(required = false) String app,
            @RequestParam(required = false) String org) {
        
        String targetApp = (app != null && !app.isBlank()) ? app.trim().toLowerCase() : "assets";
        var configOpt = appRegistrationConfigRepository != null 
                ? appRegistrationConfigRepository.findByAppName(targetApp) 
                : Optional.<com.kuleanasystems.auth.entity.AppRegistrationConfig>empty();

        String displayTitle = "Kuleana Ecosystem";
        boolean requireCaptcha = true;
        List<String> defaultRoles = List.of("viewer");

        if (configOpt.isPresent()) {
            var cfg = configOpt.get();
            displayTitle = cfg.getDisplayTitle();
            requireCaptcha = Boolean.TRUE.equals(cfg.getRequireCaptcha());
            if (cfg.getDefaultRoles() != null && !cfg.getDefaultRoles().isEmpty()) {
                defaultRoles = cfg.getDefaultRoles();
            }
        }

        String orgSlug = null;
        String orgName = null;
        boolean allowReg = true;

        if (organizationRepository != null) {
            Optional<Organization> targetOrgOpt = Optional.empty();
            if (org != null && !org.isBlank()) {
                targetOrgOpt = organizationRepository.findBySlug(org.trim().toLowerCase());
            }
            if (targetOrgOpt.isEmpty()) {
                targetOrgOpt = organizationRepository.findFirstByIsDefaultTrue()
                        .or(() -> organizationRepository.findBySlug("default-org"))
                        .or(() -> organizationRepository.findById(UUID.fromString("a0000000-0000-0000-0000-000000000001")));
            }

            if (targetOrgOpt.isPresent()) {
                var o = targetOrgOpt.get();
                orgSlug = o.getSlug();
                orgName = o.getName();
                allowReg = !Boolean.FALSE.equals(o.getAllowPublicRegistration());
            }
        }

        var response = com.kuleanasystems.auth.dto.RegistrationConfigResponse.builder()
                .appName(targetApp)
                .displayTitle(displayTitle)
                .requireCaptcha(requireCaptcha)
                .turnstileSiteKey(turnstileSiteKey)
                .organizationSlug(orgSlug)
                .organizationName(orgName)
                .defaultRoles(defaultRoles)
                .allowRegistration(allowReg)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        if (user != null) {
            try {
                authService.checkLockoutAndExpiration(user);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("permanently locked") || e.getMessage().contains("locked")) {
                    return ResponseEntity.status(423).body(e.getMessage());
                }
                return ResponseEntity.status(401).body(e.getMessage());
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            if (user != null) {
                authService.handleFailedLogin(loginRequest.getUsername());
            }
            throw ex;
        }

        if (user != null) {
            authService.handleSuccessfulLogin(loginRequest.getUsername());
        }

        boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> adminScopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();

        List<Map<String, Object>> orgs = Collections.emptyList();
        UUID activeOrgId = null;
        if (user != null) {
            orgs = getUserOrganizations(user.getId(), isSuper);
            if (!orgs.isEmpty()) {
                activeOrgId = (UUID) orgs.get(0).get("id");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String orgIdsClaim = orgs.stream().map(o -> String.valueOf(o.get("id"))).collect(Collectors.joining(","));
        String jwt = tokenProvider.generateToken(authentication.getName(), roles, activeOrgId != null ? activeOrgId.toString() : null, orgIdsClaim);

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("username", authentication.getName());
        body.put("roles", roles);
        body.put("token", jwt);
        if (user != null) {
            body.put("firstName", user.getFirstName());
            body.put("lastName", user.getLastName());
            body.put("forcePasswordChange", user.getForcePasswordChange());

            List<String> apps = new ArrayList<>();
            if (userApplicationRepository != null) {
                apps = userApplicationRepository.findByUserId(user.getId()).stream()
                        .filter(ua -> "active".equalsIgnoreCase(ua.getStatus()))
                        .map(UserApplication::getAppName)
                        .collect(Collectors.toList());
            }
            if (apps.isEmpty() && roles != null) {
                Set<String> inferred = new HashSet<>();
                String[] roleArray = roles.split(",");
                for (String r : roleArray) {
                    String rLower = r.trim().toLowerCase();
                    if (rLower.equals("role_admin") || rLower.equals("role_owner") || rLower.equals("admin") || rLower.equals("owner")) inferred.add("admin");
                    if (rLower.contains("asset")) inferred.add("assets");
                    if (rLower.contains("viewer") || rLower.contains("editor") || rLower.equals("owner") || rLower.equals("role_owner")) inferred.add("tracker");
                }
                if ("keolawong".equalsIgnoreCase(user.getUsername())) {
                    inferred.add("admin");
                    inferred.add("assets");
                }
                apps = new ArrayList<>(inferred);
            }
            body.put("apps", apps);
            body.put("isSuperAdmin", isSuper);
            body.put("adminScopes", adminScopes);
            if (userApplicationRepository != null) {
                List<Map<String, Object>> appDetails = userApplicationRepository.findByUserId(user.getId()).stream()
                        .map(ua -> Map.<String, Object>of(
                                "appName", ua.getAppName(),
                                "status", ua.getStatus(),
                                "primaryRole", ua.getPrimaryRole() != null ? ua.getPrimaryRole() : ""
                        ))
                        .collect(Collectors.toList());
                body.put("applicationDetails", appDetails);
            }
            body.put("organizations", orgs);
            body.put("activeOrgId", activeOrgId != null ? activeOrgId.toString() : null);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    @PostMapping("/register")
    @jakarta.transaction.Transactional
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            Map<String, Object> conflictResp = new HashMap<>();
            conflictResp.put("accountExists", true);
            conflictResp.put("username", request.getUsername().trim());
            conflictResp.put("message", "An account with username '" + request.getUsername().trim() + "' already exists in Kule Solutions.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResp);
        }

        // Validate Captcha
        String targetApp = (request.getAppName() != null && !request.getAppName().isBlank()) 
                ? request.getAppName().trim().toLowerCase() : "assets";
        boolean checkCaptcha = true;
        if (appRegistrationConfigRepository != null) {
            checkCaptcha = appRegistrationConfigRepository.findByAppName(targetApp)
                    .map(com.kuleanasystems.auth.entity.AppRegistrationConfig::getRequireCaptcha)
                    .orElse(true);
        }

        if (checkCaptcha) {
            String clientIp = servletRequest.getHeader("X-Forwarded-For");
            if (clientIp != null && clientIp.contains(",")) {
                clientIp = clientIp.split(",")[0].trim();
            }
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = servletRequest.getRemoteAddr();
            }
            boolean captchaPassed = captchaVerificationService.verifyCaptcha(request.getCaptchaToken(), clientIp);
            if (!captchaPassed) {
                return ResponseEntity.badRequest().body("Error: Captcha verification failed. Please try again.");
            }
        }

        try {
            PasswordValidator.validate(request.getPassword(), request.getUsername(), request.getFirstName(), request.getLastName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        if (request.getSecurityQuestions() == null || request.getSecurityQuestions().size() < 3) {
            return ResponseEntity.badRequest().body("Error: 3 security questions are required.");
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .build();

        List<UserSecurityQuestion> questions = new ArrayList<>();
        for (RegistrationRequest.SecurityQuestionDto q : request.getSecurityQuestions()) {
            questions.add(UserSecurityQuestion.builder()
                .user(user)
                .question(q.getQuestion().trim())
                .answerHash(passwordEncoder.encode(q.getAnswer().trim().toLowerCase()))
                .build());
        }
        user.setSecurityQuestions(questions);

        userRepository.save(user);

        // Sub-application assignment with default roles
        String primaryRole = "VIEWER";
        Set<String> rolesToAssign = new HashSet<>();

        if (appRegistrationConfigRepository != null) {
            var appConfig = appRegistrationConfigRepository.findByAppName(targetApp);
            if (appConfig.isPresent()) {
                primaryRole = appConfig.get().getDefaultPrimaryRole();
                if (appConfig.get().getDefaultRoles() != null) {
                    rolesToAssign.addAll(appConfig.get().getDefaultRoles());
                }
            }
        }

        if (userApplicationRepository != null) {
            userApplicationRepository.save(UserApplication.builder()
                    .userId(user.getId())
                    .appName(targetApp)
                    .status("active")
                    .primaryRole(primaryRole)
                    .build());
            
            // Also ensure default entry for complementary app if needed
            if (!"assets".equals(targetApp)) {
                userApplicationRepository.save(UserApplication.builder()
                        .userId(user.getId())
                        .appName("assets")
                        .status("active")
                        .primaryRole("OPERATOR")
                        .build());
            }
        }

        // Organization assignment with org-specific default roles
        Organization targetOrg = null;
        if (userOrganizationRepository != null && organizationRepository != null) {
            String orgSlug = request.getOrganizationSlug() != null ? request.getOrganizationSlug().trim().toLowerCase() : "";

            if (!orgSlug.isEmpty()) {
                targetOrg = organizationRepository.findBySlug(orgSlug).orElse(null);
            }
            if (targetOrg == null) {
                targetOrg = organizationRepository.findFirstByIsDefaultTrue()
                        .or(() -> organizationRepository.findBySlug("default-org"))
                        .or(() -> organizationRepository.findById(UUID.fromString("a0000000-0000-0000-0000-000000000001")))
                        .orElse(null);
            }

            if (targetOrg != null) {
                if (Boolean.FALSE.equals(targetOrg.getAllowPublicRegistration())) {
                    throw new RuntimeException("Public registration is disabled for this organization.");
                }

                userOrganizationRepository.save(UserOrganization.builder()
                        .userId(user.getId())
                        .orgId(targetOrg.getId())
                        .role("MEMBER")
                        .build());
            }
        }

        // Extract organization default application roles if available
        if (targetOrg != null && targetOrg.getDefaultApplicationRoles() != null) {
            var orgRolesJson = targetOrg.getDefaultApplicationRoles();
            if (orgRolesJson.has(targetApp) && orgRolesJson.get(targetApp).isArray()) {
                for (var element : orgRolesJson.get(targetApp)) {
                    rolesToAssign.add(element.asText());
                }
            }
        }

        // Fallback default roles if empty
        if (rolesToAssign.isEmpty()) {
            if ("assets".equals(targetApp)) {
                rolesToAssign.addAll(List.of("ASSET-READ", "ASSET-WRITE", "viewer"));
            } else {
                rolesToAssign.add("viewer");
            }
        }

        // Assign User Roles (Normalizing role names uppercase / trimmed)
        if (userRoleRepository != null && roleRepository != null) {
            List<UserRole> rolesList = new ArrayList<>();
            for (String rName : rolesToAssign) {
                String normalized = rName.trim().toUpperCase();
                // Prevent escalation to admin/superadmin via registration defaults
                if (normalized.contains("ADMIN") || normalized.contains("SUPER") || normalized.contains("OWNER")) {
                    continue;
                }
                roleRepository.findByName(normalized).or(() -> roleRepository.findByName(rName)).ifPresent(role -> {
                    rolesList.add(UserRole.builder()
                            .userId(user.getId())
                            .roleId(role.getId())
                            .status("active")
                            .build());
                });
            }
            if (!rolesList.isEmpty()) {
                userRoleRepository.saveAll(rolesList);
            }
        }

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/link-account")
    @jakarta.transaction.Transactional
    public ResponseEntity<?> linkAccount(
            @RequestBody com.kuleanasystems.auth.dto.LinkAccountRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        User user = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            user = userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        if (user == null) {
            if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body("Username and password are required to link an existing account.");
            }

            try {
                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
                );
                user = userRepository.findByUsername(auth.getName()).orElse(null);
            } catch (BadCredentialsException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        String targetApp = (request.getAppName() != null && !request.getAppName().isBlank())
                ? request.getAppName().trim().toLowerCase() : "assets";

        // Validate Captcha if user was not already authenticated
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            boolean checkCaptcha = true;
            if (appRegistrationConfigRepository != null) {
                checkCaptcha = appRegistrationConfigRepository.findByAppName(targetApp)
                        .map(com.kuleanasystems.auth.entity.AppRegistrationConfig::getRequireCaptcha)
                        .orElse(true);
            }
            if (checkCaptcha) {
                String clientIp = servletRequest.getHeader("X-Forwarded-For");
                if (clientIp != null && clientIp.contains(",")) clientIp = clientIp.split(",")[0].trim();
                if (clientIp == null || clientIp.isBlank()) clientIp = servletRequest.getRemoteAddr();
                boolean captchaPassed = captchaVerificationService.verifyCaptcha(request.getCaptchaToken(), clientIp);
                if (!captchaPassed) {
                    return ResponseEntity.badRequest().body("Captcha verification failed. Please try again.");
                }
            }
        }

        // Resolve target Organization
        Organization targetOrg = null;
        if (organizationRepository != null) {
            String orgSlug = request.getOrganizationSlug() != null ? request.getOrganizationSlug().trim().toLowerCase() : "";
            if (!orgSlug.isEmpty()) {
                targetOrg = organizationRepository.findBySlug(orgSlug).orElse(null);
            }
            if (targetOrg == null) {
                targetOrg = organizationRepository.findFirstByIsDefaultTrue()
                        .or(() -> organizationRepository.findBySlug("default-org"))
                        .or(() -> organizationRepository.findById(UUID.fromString("a0000000-0000-0000-0000-000000000001")))
                        .orElse(null);
            }
        }

        if (targetOrg == null) {
            return ResponseEntity.badRequest().body("Target organization could not be resolved.");
        }

        if (Boolean.FALSE.equals(targetOrg.getAllowPublicRegistration())) {
            boolean alreadyMember = userOrganizationRepository != null &&
                    userOrganizationRepository.findByUserIdAndOrgId(user.getId(), targetOrg.getId()).isPresent();
            if (!alreadyMember) {
                return ResponseEntity.badRequest().body("Public registration is disabled for this organization.");
            }
        }

        // 1. Link to Organization
        if (userOrganizationRepository != null) {
            var existingUO = userOrganizationRepository.findByUserIdAndOrgId(user.getId(), targetOrg.getId());
            if (existingUO.isEmpty()) {
                userOrganizationRepository.save(UserOrganization.builder()
                        .userId(user.getId())
                        .orgId(targetOrg.getId())
                        .role("MEMBER")
                        .build());
            }
        }

        // 2. Link to Sub-Application
        String primaryRole = "VIEWER";
        Set<String> rolesToAssign = new HashSet<>();

        if (appRegistrationConfigRepository != null) {
            var appConfig = appRegistrationConfigRepository.findByAppName(targetApp);
            if (appConfig.isPresent()) {
                primaryRole = appConfig.get().getDefaultPrimaryRole();
                if (appConfig.get().getDefaultRoles() != null) {
                    rolesToAssign.addAll(appConfig.get().getDefaultRoles());
                }
            }
        }

        if (userApplicationRepository != null) {
            var existingApp = userApplicationRepository.findByUserIdAndAppName(user.getId(), targetApp);
            if (existingApp.isEmpty()) {
                userApplicationRepository.save(UserApplication.builder()
                        .userId(user.getId())
                        .appName(targetApp)
                        .status("active")
                        .primaryRole(primaryRole)
                        .build());
            } else if (!"active".equalsIgnoreCase(existingApp.get().getStatus())) {
                var ua = existingApp.get();
                ua.setStatus("active");
                userApplicationRepository.save(ua);
            }
        }

        // 3. Organization default roles
        if (targetOrg.getDefaultApplicationRoles() != null) {
            var orgRolesJson = targetOrg.getDefaultApplicationRoles();
            if (orgRolesJson.has(targetApp) && orgRolesJson.get(targetApp).isArray()) {
                for (var elem : orgRolesJson.get(targetApp)) {
                    rolesToAssign.add(elem.asText());
                }
            }
        }

        if (rolesToAssign.isEmpty()) {
            if ("assets".equals(targetApp)) {
                rolesToAssign.addAll(List.of("ASSET-READ", "ASSET-WRITE", "viewer"));
            } else {
                rolesToAssign.add("viewer");
            }
        }

        // 4. Assign default roles (preventing admin escalation)
        final UUID finalUserId = user.getId();
        if (userRoleRepository != null && roleRepository != null) {
            List<UserRole> existingRoles = userRoleRepository.findByUserId(finalUserId);
            Set<UUID> existingRoleIds = existingRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());

            List<UserRole> newRolesList = new ArrayList<>();
            for (String rName : rolesToAssign) {
                String normalized = rName.trim().toUpperCase();
                if (normalized.contains("ADMIN") || normalized.contains("SUPER") || normalized.contains("OWNER")) {
                    continue;
                }
                roleRepository.findByName(normalized).or(() -> roleRepository.findByName(rName)).ifPresent(role -> {
                    if (!existingRoleIds.contains(role.getId())) {
                        newRolesList.add(UserRole.builder()
                                .userId(finalUserId)
                                .roleId(role.getId())
                                .status("active")
                                .build());
                    }
                });
            }
            if (!newRolesList.isEmpty()) {
                userRoleRepository.saveAll(newRolesList);
            }
        }

        // 5. Generate fresh token
        boolean isSuper = userRoleRepository != null && userRoleRepository.findByUserId(user.getId()).stream()
                .anyMatch(ur -> {
                    String rName = ur.getRole() != null ? ur.getRole().getName() : "";
                    return "SUPERADMIN".equalsIgnoreCase(rName) || "ROLE_SUPERADMIN".equalsIgnoreCase(rName) || "owner".equalsIgnoreCase(rName);
                });

        List<Map<String, Object>> orgs = getUserOrganizations(user.getId(), isSuper);
        String orgIdsClaim = orgs.stream().map(o -> String.valueOf(o.get("id"))).collect(Collectors.joining(","));
        
        List<String> userRoles = new ArrayList<>();
        if (userRoleRepository != null) {
            userRoles = userRoleRepository.findByUserId(user.getId()).stream()
                    .map(ur -> ur.getRole() != null ? ur.getRole().getName() : "")
                    .filter(r -> !r.isBlank())
                    .collect(Collectors.toList());
        }
        String rolesStr = String.join(",", userRoles);

        String jwt = tokenProvider.generateToken(user.getUsername(), rolesStr, targetOrg.getId().toString(), orgIdsClaim);

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Account successfully linked to " + targetOrg.getName() + " for " + targetApp + "!");
        resp.put("username", user.getUsername());
        resp.put("organizationName", targetOrg.getName());
        resp.put("organizationSlug", targetOrg.getSlug());
        resp.put("appName", targetApp);
        resp.put("token", jwt);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(resp);
    }

    public static final List<String> SECURITY_QUESTION_POOL = List.of(
        "What was the name of your first pet?",
        "What is your mother's maiden name?",
        "What city were you born in?",
        "What is the name of your first school?",
        "What is your favorite book?",
        "What is your favorite movie?",
        "What is the name of your favorite teacher?",
        "What was the make and model of your first car?",
        "What high school did you attend?",
        "What was your childhood nickname?",
        "In what city did your parents meet?",
        "What was the name of your first employer or job?",
        "What was the street you lived on in elementary school?",
        "What was the name of your childhood best friend?",
        "What was the first live concert you attended?",
        "What was the mascot of your high school or college?",
        "What was your favorite food or meal growing up?",
        "What was the name of the town where your grandparents lived?",
        "What was the destination of your first family road trip?",
        "What was the name of your first stuffed animal or toy?"
    );

    @GetMapping("/security-questions/pool")
    public ResponseEntity<List<String>> getSecurityQuestionPool() {
        return ResponseEntity.ok(SECURITY_QUESTION_POOL);
    }

    @GetMapping("/security-questions/challenge/{username}")
    public ResponseEntity<?> getChallengeQuestions(@PathVariable String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<UserSecurityQuestion> allQuestions = userSecurityQuestionRepository.findByUserId(user.getId());
        if (allQuestions.size() < 2) {
            return ResponseEntity.badRequest().body("User does not have enough security questions configured.");
        }

        List<UserSecurityQuestion> shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled);
        List<String> questions = shuffled.stream().limit(2).map(UserSecurityQuestion::getQuestion).collect(Collectors.toList());

        return ResponseEntity.ok(questions);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String newPassword = (String) request.get("newPassword");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> answers = (List<Map<String, String>>) request.get("answers");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<UserSecurityQuestion> allQuestions = userSecurityQuestionRepository.findByUserId(user.getId());

        int correctCount = 0;
        if (answers != null) {
            for (Map<String, String> answerDto : answers) {
                String q = answerDto.get("question");
                String a = answerDto.get("answer") != null ? answerDto.get("answer").toLowerCase() : "";

                for (UserSecurityQuestion usq : allQuestions) {
                    if (usq.getQuestion().equals(q) && passwordEncoder.matches(a, usq.getAnswerHash())) {
                        correctCount++;
                        break;
                    }
                }
            }
        }

        if (correctCount < 2) {
            authService.handleFailedLogin(username);
            return ResponseEntity.badRequest().body("Failed to verify security questions.");
        }

        try {
            PasswordValidator.validate(newPassword, user.getUsername(), user.getFirstName(), user.getLastName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockoutRound(0);
        user.setAccountLockedUntil(null);
        user.setForcePasswordChange(false);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);
        userAuthCacheService.evictUser(username);

        return ResponseEntity.ok("Password reset successfully.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication, @RequestBody Map<String, String> request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Not authenticated");

        String newPassword = request.get("newPassword");
        try {
            PasswordValidator.validate(newPassword, user.getUsername(), user.getFirstName(), user.getLastName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);
        userAuthCacheService.evictUser(user.getUsername());

        return ResponseEntity.ok("Password changed successfully.");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody Map<String, String> request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Not authenticated");

        if (request.containsKey("firstName")) {
            user.setFirstName(request.get("firstName"));
        }
        if (request.containsKey("lastName")) {
            user.setLastName(request.get("lastName"));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Profile updated successfully.");
    }

    @PutMapping("/security-questions")
    public ResponseEntity<?> updateSecurityQuestions(Authentication authentication, @RequestBody List<RegistrationRequest.SecurityQuestionDto> request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        if (request == null || request.size() < 3) {
            return ResponseEntity.badRequest().body("3 security questions are required.");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Not authenticated");

        List<UserSecurityQuestion> existing = userSecurityQuestionRepository.findByUserId(user.getId());
        Map<String, String> existingHashByQuestion = existing.stream()
                .collect(Collectors.toMap(UserSecurityQuestion::getQuestion, UserSecurityQuestion::getAnswerHash, (a, b) -> a));

        List<UserSecurityQuestion> newQuestions = new ArrayList<>();
        for (RegistrationRequest.SecurityQuestionDto q : request) {
            String answerHash;
            if (StringUtils.hasText(q.getAnswer())) {
                answerHash = passwordEncoder.encode(q.getAnswer().trim().toLowerCase());
            } else if (existingHashByQuestion.containsKey(q.getQuestion())) {
                answerHash = existingHashByQuestion.get(q.getQuestion());
            } else {
                return ResponseEntity.badRequest().body("Answer is required for new security question: " + q.getQuestion());
            }
            newQuestions.add(UserSecurityQuestion.builder()
                    .user(user)
                    .question(q.getQuestion())
                    .answerHash(answerHash)
                    .build());
        }

        userSecurityQuestionRepository.deleteAll(existing);
        userSecurityQuestionRepository.saveAll(newQuestions);
        return ResponseEntity.ok("Security questions updated successfully.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String token = getJwtFromRequest(request);
        if (StringUtils.hasText(token)) {
            tokenBlacklistService.blacklist(token);
            if (tokenRevocationService != null) {
                tokenRevocationService.revokeToken(token, null);
            }
        }

        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Successfully logged out.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body("Not authenticated");

        String roles = "";
        if (userRoleRepository != null) {
            List<String> dynamicRoles = new ArrayList<>();
            userRoleRepository.findByUserId(user.getId()).stream()
                    .filter(ur -> "active".equalsIgnoreCase(ur.getStatus()))
                    .forEach(ur -> {
                        if (ur.getRole() != null && ur.getRole().getName() != null) {
                            String roleName = ur.getRole().getName().toUpperCase();
                            dynamicRoles.add("ROLE_" + roleName);
                            dynamicRoles.add(roleName);
                        }
                        if (rolePageRepository != null) {
                            rolePageRepository.findByRoleId(ur.getRoleId()).stream()
                                    .filter(rp -> Boolean.TRUE.equals(rp.getIsActive()) && rp.getPage() != null)
                                    .forEach(rp -> dynamicRoles.add("PAGE_" + rp.getPage().getPath()));
                        }
                    });
            if (!dynamicRoles.isEmpty()) {
                roles = dynamicRoles.stream().distinct().collect(Collectors.joining(","));
            }
        }

        if (roles.isEmpty() && authentication.getAuthorities() != null) {
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("username", authentication.getName());
        body.put("roles", roles);
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());

        List<String> apps = new ArrayList<>();
        if (userApplicationRepository != null) {
            apps = userApplicationRepository.findByUserId(user.getId()).stream()
                    .filter(ua -> "active".equalsIgnoreCase(ua.getStatus()))
                    .map(UserApplication::getAppName)
                    .collect(Collectors.toList());
        }
        if (apps.isEmpty() && roles != null) {
            Set<String> inferred = new HashSet<>();
            String[] roleArray = roles.split(",");
            for (String r : roleArray) {
                String rLower = r.trim().toLowerCase();
                if (rLower.equals("role_admin") || rLower.equals("role_owner") || rLower.equals("admin") || rLower.equals("owner")) inferred.add("admin");
                if (rLower.contains("asset")) inferred.add("assets");
                if (rLower.contains("viewer") || rLower.contains("editor") || rLower.equals("owner") || rLower.equals("role_owner")) inferred.add("tracker");
            }
            if ("keolawong".equalsIgnoreCase(user.getUsername())) {
                inferred.add("admin");
                inferred.add("assets");
            }
            apps = new ArrayList<>(inferred);
        }
        body.put("apps", apps);

        List<UserSecurityQuestion> userQuestions = userSecurityQuestionRepository.findByUserId(user.getId());
        List<String> questionTexts = userQuestions.stream()
                .map(UserSecurityQuestion::getQuestion)
                .collect(Collectors.toList());
        body.put("securityQuestions", questionTexts);
        boolean isSuperMe = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
        List<String> adminScopesMe = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();
        body.put("isSuperAdmin", isSuperMe);
        body.put("adminScopes", adminScopesMe);
        if (userApplicationRepository != null) {
            List<Map<String, Object>> appDetails = userApplicationRepository.findByUserId(user.getId()).stream()
                    .map(ua -> Map.<String, Object>of(
                            "appName", ua.getAppName(),
                            "status", ua.getStatus(),
                            "primaryRole", ua.getPrimaryRole() != null ? ua.getPrimaryRole() : ""
                    ))
                    .collect(Collectors.toList());
            body.put("applicationDetails", appDetails);
        }

        List<Map<String, Object>> orgs = getUserOrganizations(user.getId(), isSuperMe);
        UUID activeOrgId = !orgs.isEmpty() ? (UUID) orgs.get(0).get("id") : null;
        body.put("organizations", orgs);
        body.put("activeOrgId", activeOrgId != null ? activeOrgId.toString() : null);

        return ResponseEntity.ok(body);
    }

    private List<Map<String, Object>> getUserOrganizations(UUID userId, boolean isSuperAdmin) {
        if (organizationRepository == null || userId == null) {
            return Collections.emptyList();
        }
        if (isSuperAdmin) {
            List<Organization> allOrgs = organizationRepository.findAll();
            Map<UUID, String> roleByOrgId = userOrganizationRepository != null
                    ? userOrganizationRepository.findByUserId(userId).stream()
                            .collect(Collectors.toMap(UserOrganization::getOrgId, UserOrganization::getRole, (a, b) -> a))
                    : Collections.emptyMap();

            return allOrgs.stream()
                    .map(org -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", org.getId());
                        map.put("slug", org.getSlug());
                        map.put("name", org.getName());
                        map.put("role", roleByOrgId.getOrDefault(org.getId(), "ORG_ADMIN"));
                        map.put("status", org.getStatus());
                        return map;
                    })
                    .collect(Collectors.toList());
        }

        if (userOrganizationRepository == null) {
            return Collections.emptyList();
        }
        List<UserOrganization> userOrgs = userOrganizationRepository.findByUserId(userId);
        if (userOrgs.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> orgIds = userOrgs.stream().map(UserOrganization::getOrgId).collect(Collectors.toList());
        Map<UUID, Organization> orgMap = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, o -> o));

        return userOrgs.stream()
                .map(uo -> {
                    Organization org = orgMap.get(uo.getOrgId());
                    if (org == null) return null;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", org.getId());
                    map.put("slug", org.getSlug());
                    map.put("name", org.getName());
                    map.put("role", uo.getRole());
                    map.put("status", org.getStatus());
                    return map;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        return getCurrentUser(authentication);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
