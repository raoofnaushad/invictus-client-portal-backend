package com.asbitech.client_ms.infra.external.keycloak.dto;

import lombok.Builder;

@Builder
public record AuthRequest(String username, String password) {

}
