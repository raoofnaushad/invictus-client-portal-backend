package com.asbitech.client_ms.infra.external.keycloak.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
public record UserRequest(
        String username,
        String email,
        boolean enabled,
        String firstName,
        String lastName,
        List<Credential> credentials
) {
    public static UserRequest withTempPassword(String username, String email, String firstName, String lastName, String password) {
        System.out.println("Creating user request with temp password: " + password);
        
        return new UserRequest(
                username,
                email,
                true, // User is enabled
                firstName,
                lastName,
                List.of(new Credential("password", password, true))
        );
    }
}
