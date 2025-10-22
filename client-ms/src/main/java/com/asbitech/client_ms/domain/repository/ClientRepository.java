package com.asbitech.client_ms.domain.repository;


import java.util.Optional;

import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.entity.UserCredential;
import com.asbitech.client_ms.infra.external.keycloak.dto.TokenResponse;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface ClientRepository {
    Mono<Either<CustomError, UserCredential>> generateUserCredential(String username, String firstName, String lastName, String email);
    Mono<Either<CustomError, Principal>> loadByActivationtoken(String activationToken);
    Mono<Either<CustomError, Principal>> loadByIamUserId(String userId);
    Mono<Either<CustomError, TokenResponse>> authenticateUser(String username, String password);
    Mono<Either<CustomError, Void>> updatePassword(String iammUserId, String newPassword);
    Mono<Either<CustomError, Principal>> store(Principal principal);
}
