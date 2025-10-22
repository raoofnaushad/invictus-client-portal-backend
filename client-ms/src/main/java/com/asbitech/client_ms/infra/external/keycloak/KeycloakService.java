package com.asbitech.client_ms.infra.external.keycloak;

import java.util.Optional;

import com.asbitech.client_ms.infra.external.keycloak.dto.AuthRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.Credential;
import com.asbitech.client_ms.infra.external.keycloak.dto.TokenResponse;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserResponse;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface KeycloakService {
    Mono<Either<CustomError, UserResponse>> createKeycloakUser(UserRequest userRequest);
    Mono<Either<CustomError, Void>> updateUserPassword(String keycloakUserId, Credential creds);
    Mono<Either<CustomError,TokenResponse>> authenticateUser(AuthRequest authRequest);
    Mono<Either<CustomError,TokenResponse>> refreshToken(String username, String refreshToken);
    Mono<Either<CustomError, UserResponse>> getUser(String userId);
}
