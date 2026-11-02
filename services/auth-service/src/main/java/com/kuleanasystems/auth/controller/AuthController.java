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

        List<Map<String, Object>> orgs = Collections.emptyList();
        UUID activeOrgId = null;
        if (user != null) {
            orgs = getUserOrganizations(user.getId());
            if (!orgs.isEmpty()) {
                activeOrgId = (UUID) orgs.get(0).get("id");
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication, activeOrgId != null ? activeOrgId.toString() : null);

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

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
            boolean isSuper = adminScopeService != null && adminScopeService.isSuperAdmin(authentication);
            List<String> adminScopes = adminScopeService != null ? adminScopeService.getAdminScopes(authentication) : List.of();
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
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
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
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        List<UserSecurityQuestion> questions = new ArrayList<>();
        for (RegistrationRequest.SecurityQuestionDto q : request.getSecurityQuestions()) {
            questions.add(UserSecurityQuestion.builder()
                .user(user)
                .question(q.getQuestion())
                .answerHash(passwordEncoder.encode(q.getAnswer().toLowerCase()))
                .build());
        }
        user.setSecurityQuestions(questions);

        userRepository.save(user);

        if (userApplicationRepository != null) {
            userApplicationRepository.save(UserApplication.builder()
                    .userId(user.getId())
                    .appName("assets")
                    .status("active")
                    .primaryRole("OPERATOR")
                    .build());
        }

        if (userApplicationRepository != null) {
            userApplicationRepository.save(UserApplication.builder()
                    .userId(user.getId())
                    .appName("tracker")
                    .status("active")
                    .primaryRole("VIEWER")
                    .build());
        }

        if (userRoleRepository != null && roleRepository != null) {
            List<String> defaultRoles = List.of("ASSET-READ", "ASSET-WRITE", "viewer");
            List<UserRole> rolesList = new ArrayList<>();
            for (String rName : defaultRoles) {
                roleRepository.findByName(rName).ifPresent(role -> {
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

        List<Map<String, Object>> orgs = getUserOrganizations(user.getId());
        UUID activeOrgId = !orgs.isEmpty() ? (UUID) orgs.get(0).get("id") : null;
        body.put("organizations", orgs);
        body.put("activeOrgId", activeOrgId != null ? activeOrgId.toString() : null);

        return ResponseEntity.ok(body);
    }

    private List<Map<String, Object>> getUserOrganizations(UUID userId) {
        if (userOrganizationRepository == null || organizationRepository == null || userId == null) {
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
