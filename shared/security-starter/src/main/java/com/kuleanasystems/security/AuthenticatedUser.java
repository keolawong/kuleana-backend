package com.kuleanasystems.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser implements Serializable {

    private UUID id;
    private String username;
    private String email;
    private List<String> roles;
}
