package com.kahua.tracker.security;

import com.kahua.tracker.entity.User;
import com.kahua.tracker.repository.UserRepository;
import com.kahua.tracker.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private com.kahua.tracker.repository.RolePageRepository rolePageRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByUserId(user.getId()).stream()
                .filter(userRole -> "active".equalsIgnoreCase(userRole.getStatus()))
                .flatMap(userRole -> {
                    java.util.List<SimpleGrantedAuthority> auths = new java.util.ArrayList<>();
                    auths.add(new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName().toUpperCase()));
                    
                    rolePageRepository.findByRoleId(userRole.getRoleId()).stream()
                            .filter(rp -> Boolean.TRUE.equals(rp.getIsActive()))
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
