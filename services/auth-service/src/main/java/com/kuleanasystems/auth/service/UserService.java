package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.dto.AuthResponse;
import com.kuleanasystems.auth.dto.LoginRequest;
import com.kuleanasystems.auth.dto.RegisterRequest;
import com.kuleanasystems.auth.dto.UserDto;
import com.kuleanasystems.auth.entity.Role;
import com.kuleanasystems.auth.entity.Token;
import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.repository.RoleRepository;
import com.kuleanasystems.auth.repository.TokenRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import com.kuleanasystems.security.JwtTokenProvider;
import com.kuleanasystems.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName().toUpperCase()))
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountNonLocked(!Boolean.TRUE.equals(user.getIsLocked()))
                .enabled(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new BadCredentialsException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setIsLocked(true);
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        String roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(","));

        String token = jwtTokenProvider.generateToken(user.getUsername(), roles, user.getId(), user.getEmail());

        Token tokenEntity = Token.builder()
                .token(token)
                .tokenType("BEARER")
                .username(user.getUsername())
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .isRevoked(false)
                .isExpired(false)
                .build();
        tokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToDto(user))
                .build();
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                String normalized = roleName.startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();
                Role role = roleRepository.findByName(normalized)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(normalized).description("Auto-provisioned role").build()));
                roles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").description("Standard user").build()));
            roles.add(defaultRole);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(roles)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokenRepository.findByToken(token).ifPresent(t -> {
                t.setIsRevoked(true);
                tokenRepository.save(t);
            });
        }
    }

    public UserDto mapToDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .isLocked(user.getIsLocked())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
