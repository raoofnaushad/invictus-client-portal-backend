package com.asbitech.client_ms.infra.external.keycloak.dto;

import lombok.Builder;

@Builder
public record Credential(String type, String value, boolean temporary) {}

