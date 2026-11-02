package com.kuleanasystems.auth.service;

import com.kuleanasystems.auth.entity.User;
import com.kuleanasystems.auth.repository.RolePageRepository;
import com.kuleanasystems.auth.repository.UserRepository;
import com.kuleanasystems.auth.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePageRepository rolePageRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByUserId(user.getId()).stream()
                .filter(userRole -> "active".equalsIgnoreCase(userRole.getStatus()))
                .flatMap(userRole -> {
                    List<SimpleGrantedAuthority> auths = new ArrayList<>();
                    if (userRole.getRole() != null && userRole.getRole().getName() != null) {
                        auths.add(new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName().toUpperCase()));
                    }

                    rolePageRepository.findByRoleId(userRole.getRoleId()).stream()
                            .filter(rp -> Boolean.TRUE.equals(rp.getIsActive()) && rp.getPage() != null)
                            .forEach(rp -> auths.add(new SimpleGrantedAuthority("PAGE_" + rp.getPage().getPath())));

                    return auths.stream();
                })
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
