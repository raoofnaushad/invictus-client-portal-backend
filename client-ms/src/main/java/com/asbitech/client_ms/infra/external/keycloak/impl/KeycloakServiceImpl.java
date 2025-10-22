package com.asbitech.client_ms.infra.external.keycloak.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.asbitech.client_ms.infra.external.keycloak.KeycloakService;
import com.asbitech.client_ms.infra.external.keycloak.dto.AuthRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.Credential;
import com.asbitech.client_ms.infra.external.keycloak.dto.TokenResponse;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserResponse;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

@Service
public class KeycloakServiceImpl implements KeycloakService {
    private static final String BEARER_PREFIX = "Bearer ";
    private final WebClient webClient;

    // TODO move to properties
    private final String keycloakBaseUrl = "http://localhost:8080";
    private final String realm = "client-portal";
    private final String clientId = "admin-client";
    private final String clientSecret = "hKE6u1vhwdbOxTzZUYCNhmE25icPzABk";

    // Caching token and expiration time
    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);
    private final AtomicReference<Long> tokenExpiryTime = new AtomicReference<>(0L);

    public KeycloakServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(keycloakBaseUrl).build();
    }

    private Mono<Either<CustomError, String>> getAdminToken() {
        long currentTime = Instant.now().getEpochSecond();
        
        if (cachedToken.get() != null && currentTime < tokenExpiryTime.get()) {
            return Mono.just(Either.right(cachedToken.get())); // Return cached token if not expired
        }

        return authenticateAdmin()
            .map(response -> {
                if (response.isLeft()) {
                    return Either.left(response.getLeft()); // Return error if token retrieval fails
                } else {
                    cachedToken.set(response.get().access_token());
                    tokenExpiryTime.set(currentTime + response.get().expires_in() - 10); // buffer to avoid expiry issues

                    return Either.right(response.get().access_token()); // Return new token if successful
                }
            });
    }

    /**
     * Private method to authenticate as an admin and retrieve a new token
     */
    private Mono<Either<CustomError, TokenResponse>> authenticateAdmin() {
        return webClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue("client_id=" + clientId + "&grant_type=client_credentials&client_secret=" + clientSecret)
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .map(Either::<CustomError, TokenResponse>right)  // Return the response wrapped in Either.right
            .onErrorResume(error -> {
                // Handle any error during token retrieval and return a custom error wrapped in Either.left
                CustomError customError = handleError(error, "KEYCLOAK_TOKEN_RETRIEVAL_ERROR");
                return Mono.just(Either.left(customError));
            });
    }

    


    private CustomError handleError(Throwable error, String errorCode) {
        if (error instanceof WebClientResponseException ex) {
            WebClientResponseException werror = (WebClientResponseException) error;

           if (werror.getStatusCode() == HttpStatusCode.valueOf(401)) {
                return new CustomError(
                    HttpStatus.UNAUTHORIZED,
                    errorCode,
                    "Keycloak API error: " + ex.getResponseBodyAsString(),
                    null
                );
           }

           return new CustomError(
                HttpStatus.BAD_REQUEST,
                errorCode,
                "Keycloak API error: " + ex.getResponseBodyAsString(),
                null
            );
        }

        return new CustomError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "UNKNOWN_ERROR",
            "An unknown error occurred",
            null
        );
    }

    private Mono<Optional<CustomError>> createUser(UserRequest userRequest) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                            customError -> Mono.just(Optional.of(customError)), 
                            token -> webClient.post()
                                        .uri("/admin/realms/{realm}/users", realm)
                                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                                        .bodyValue(userRequest)
                                        .retrieve()
                                        .toBodilessEntity()
                                        .map(response -> Optional.<CustomError>empty())
                                        .onErrorResume(error -> {
                                            CustomError customError = handleError(error, "KEYCLOAK_USER_CREATION_ERROR");
                                            return Mono.just(Optional.of(customError));
                                        })
                            )
                        );
    }
    

    
    
    private Mono<Either<CustomError, List<UserResponse>>> searchUser(String userName) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                                    customError -> Mono.just(Either.left(customError)), 
                                    token -> webClient.get()
                                    .uri("/admin/realms/{realm}/users?username={userName}", realm, userName)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<List<UserResponse>>() {})
                                    .map(Either::<CustomError, List<UserResponse>>right)
                                    .onErrorResume(error -> {
                                        CustomError customError = handleError(error, "KEYCLOAK_USER_CREATION_ERROR");
                                        return Mono.just(Either.left(customError));
                                    }))
                );
    }

    @Override
    public Mono<Either<CustomError, UserResponse>> getUser(String userId) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                                    customError -> Mono.just(Either.left(customError)), 
                                    token -> webClient.get()
                                    .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                                    .retrieve()
                                    .bodyToMono(UserResponse.class)
                                    .map(Either::<CustomError, UserResponse>right)
                                    .onErrorResume(error -> {
                                        CustomError customError = handleError(error, "KEYCLOAK_USER_CREATION_ERROR");
                                        return Mono.just(Either.left(customError));
                                    }))
                );
    }




    @Override
    public Mono<Either<CustomError, UserResponse>> createKeycloakUser(UserRequest userRequest) {
        return createUser(userRequest)  // Step 1: Create user
                .flatMap(response -> {
                    if (response.isEmpty()) {
                        // If user creation is successful (no errors), retrieve the user
                        return searchUser(userRequest.username())
                            .flatMap(userResponse -> {
                                if (userResponse.isRight()) {
                                    // If user retrieval is successful, return the user
                                    return Mono.just(Either.right(userResponse.get().get(0)));
                                } else {
                                    // If user retrieval fails, return the error
                                    CustomError customError = userResponse.getLeft();  // Get the error from Either
                                    return Mono.just(Either.left(customError));
                                }
                            });
                    } else {
                        // If user creation fails, return the error
                        CustomError customError = response.get();  // Get the error from Optional
                        return Mono.just(Either.left(customError));
                    }
                });
    }

    @Override
    public Mono<Either<CustomError, Void>> updateUserPassword(String keycloakUserId, Credential creds) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                            customError -> Mono.just(Either.<CustomError, Void>left(customError)), 
                            token -> webClient.put()
                                        .uri("/admin/realms/{realm}/users/{userId}/reset-password", realm, keycloakUserId)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                                        .bodyValue(creds)
                                        .retrieve() 
                                        .toBodilessEntity()
                                        .flatMap(response -> Mono.just(Either.<CustomError, Void>right(null)))
                                        .onErrorResume(error -> {
                                            CustomError customError = handleError(error, "KEYCLOAK_USER_RESET_PASSWORD_ERROR");
                                            return Mono.just(Either.<CustomError, Void>left(customError));
                                        })
                            )
                        );
    }

    @Override
    public Mono<Either<CustomError,TokenResponse>> authenticateUser(AuthRequest authRequest) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                            customError -> Mono.just(Either.left(customError)), 
                            token -> webClient.post()
                                        .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                        .bodyValue("client_id=" + clientId + "&grant_type=password&client_secret=" + clientSecret + "&username=" + authRequest.username() + "&password=" + authRequest.password())
                                        .retrieve()
                                        .bodyToMono(TokenResponse.class)
                                        .map(Either::<CustomError,TokenResponse>right)
                                        .onErrorResume(error -> {
                                            System.out.print("client_id=" + clientId + "&grant_type=password&client_secret=" + clientSecret + "&username=" + authRequest.username() + "&password=" + authRequest.password());                                          CustomError customError = handleError(error, "KEYCLOAK_USER_AUTH_ERROR");
                                            return Mono.just(Either.left(customError));
                                        })
                            )
                        );
    }

    @Override
    public Mono<Either<CustomError,TokenResponse>> refreshToken(String username, String refreshToken) {
        return getAdminToken()
                .flatMap(tokenRsp -> tokenRsp.fold(
                            customError -> Mono.just(Either.left(customError)), 
                            token -> webClient.post()
                                        .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                        .bodyValue("client_id=" + clientId + "&grant_type=refresh_token&client_secret=" + clientSecret + "&username=" + username + "&refresh_token=" + refreshToken)
                                        .retrieve()
                                        .bodyToMono(TokenResponse.class)
                                        .map(Either::<CustomError,TokenResponse>right)
                                        .onErrorResume(error -> {
                                            CustomError customError = handleError(error, "KEYCLOAK_REFRESH_TOKEN_ERROR");
                                            return Mono.just(Either.left(customError));
                                        })
                            )
                        );
    }
}
