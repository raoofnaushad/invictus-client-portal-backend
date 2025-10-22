package com.asbitech.client_ms.infra.repository;


import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.entity.UserCredential;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.domain.vo.UserCredentialId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.client_ms.infra.external.keycloak.KeycloakService;
import com.asbitech.client_ms.infra.external.keycloak.dto.AuthRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.Credential;
import com.asbitech.client_ms.infra.external.keycloak.dto.TokenResponse;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserRequest;
import com.asbitech.client_ms.infra.external.keycloak.dto.UserResponse;
import com.asbitech.client_ms.infra.mapper.PrincipalMapper;
import com.asbitech.client_ms.infra.persistence.jpaRepo.PrincipalJpaRepository;
import com.asbitech.client_ms.infra.persistence.jpaRepo.UserCredentialJpaRepository;
import com.asbitech.client_ms.infra.persistence.table.PrincipalTable;
import com.asbitech.client_ms.infra.persistence.table.UserCredentialTable;
import com.asbitech.client_ms.infra.utils.PasswordUtils;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;


@Repository
public class ClientRepositoryImpl implements ClientRepository {

    private final PrincipalJpaRepository principalJpaRepository;
    private final UserCredentialJpaRepository userCredentialTableRepository;
    private final KeycloakService keycloakService;

    public ClientRepositoryImpl(PrincipalJpaRepository principalJpaRepository, KeycloakService keycloakService, UserCredentialJpaRepository userCredentialTableRepository) {
        this.principalJpaRepository = principalJpaRepository;
        this.userCredentialTableRepository = userCredentialTableRepository;
        this.keycloakService = keycloakService;
    }

    @Override
    public Mono<Either<CustomError, UserCredential>> generateUserCredential(String username, String firstName, String lastName,
            String email) {

        String activationToken = PasswordUtils.generateTempPassword(30);

        UserRequest userRequest = UserRequest.withTempPassword(email, email, firstName, lastName, activationToken);
        return keycloakService.createKeycloakUser(userRequest)
                .flatMap(response -> {
                    if (response.isRight()) {
                        UserResponse keycloakUserResp = response.get();

                        UserCredential userCredential = UserCredential.builder()
                                                            .id(new UserCredentialId())
                                                            .iamUserId(keycloakUserResp.id())
                                                            .fullName(firstName)
                                                            .username(username)
                                                            .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                                                            .isNew(true)
                                                            .createdAt(LocalDateTime.now())
                                                            .updatedAt(LocalDateTime.now())
                                                            .activationToken(activationToken)
                                                            .activationTokenExpiry(LocalDateTime.now().plusDays(2))  // TODO move to config activationLink expiry config                                                        .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                                                            .mfaActivated(false)
                                                            .build();

                        return Mono.just(Either.right(userCredential));
                    } else {
                        return Mono.just(Either.left(response.getLeft()));
                    }
                });
    }

    @Override
    public Mono<Either<CustomError, Principal>> store(Principal principal) {
        // Map domain entities to database entities
            PrincipalTable principalTable = PrincipalMapper.toTable(principal);
            UserCredentialTable userCredentialTable = PrincipalMapper.toTable(principal.getUserCredential());
            System.out.println(principal.getUserCredential());
            System.out.println(principal.getUserCredential().getId());

            System.out.println(principal.getId());


            return userCredentialTableRepository.save(userCredentialTable)
                .doOnNext(rsp -> principalTable.setUserCredentialId(rsp.getId())) // Set saved credential
                .flatMap(savedCredential -> principalJpaRepository.save(principalTable)) // Save principal
                .map(savedPrincipal -> Either.<CustomError, Principal>right(principal)) // Success case
                .onErrorResume(ex -> Mono.just(Either.<CustomError, Principal>left(new CustomError(
                        HttpStatus.BAD_REQUEST,
                        "ERROR_SAVING_PRINCIPAL",
                        "JPA saving principal error: " + ex.getMessage(),
                        null
                ))));
    }

    @Override
    public Mono<Either<CustomError, Principal>> loadByActivationtoken(String activationToken) {
        return userCredentialTableRepository.findByActivationToken(activationToken)
                                .flatMap(userCredentialTable -> {
                                    return principalJpaRepository.findByUserCredentialId(userCredentialTable.getId())
                                            .map(principalTable -> {
                                                Principal pr  = PrincipalMapper.toDomain(principalTable);
                                                UserCredential usrC = PrincipalMapper.toDomain(userCredentialTable);
                                                pr.setUserCredential(usrC);

                                                return Either.<CustomError, Principal>right(pr);
                                            })
                                            .switchIfEmpty(Mono.just(Either.left(new CustomError(
                                                    HttpStatus.NOT_FOUND,
                                                    "ACTIVATION_TOKEN_NOT_FOUND",
                                                    "No principal found with the given activation token",
                                                    null
                                            )))); // Handle empty result
                                }).switchIfEmpty(Mono.just(Either.left(new CustomError(
                                        HttpStatus.NOT_FOUND,
                                        "ACTIVATION_TOKEN_NOT_FOUND",
                                        "No user credential found with the given activation token",
                                        null
                                    ))));}

    @Override
    public Mono<Either<CustomError, Void>> updatePassword(String iammUserId, String newPassword) {
        return keycloakService.updateUserPassword(iammUserId, Credential.builder()
                .temporary(false).type("password").value(newPassword).build());
    }

    @Override
    public Mono<Either<CustomError, TokenResponse>> authenticateUser(String username, String password) {
       return keycloakService.authenticateUser(AuthRequest.builder()    
                .username(username)
                .password(password)
                .build());
    }

    @Override
    public Mono<Either<CustomError, Principal>> loadByIamUserId(String userId) {
        return userCredentialTableRepository.findByIamUserId(userId)
                                .flatMap(userCredentialTable -> {
                                    return principalJpaRepository.findByUserCredentialId(userCredentialTable.getId())
                                            .map(principalTable -> {
                                                Principal pr  = PrincipalMapper.toDomain(principalTable);
                                                UserCredential usrC = PrincipalMapper.toDomain(userCredentialTable);
                                                pr.setUserCredential(usrC);

                                                return Either.<CustomError, Principal>right(pr);
                                            })
                                            .switchIfEmpty(Mono.just(Either.left(new CustomError(
                                                    HttpStatus.NOT_FOUND,
                                                    "ACTIVATION_TOKEN_NOT_FOUND",
                                                    "No principal found with the given activation token",
                                                    null
                                            )))); // Handle empty result
                                }).switchIfEmpty(Mono.just(Either.left(new CustomError(
                                        HttpStatus.NOT_FOUND,
                                        "ACTIVATION_TOKEN_NOT_FOUND",
                                        "No user credential found with the given activation token",
                                        null
                                    ))));
                }
}