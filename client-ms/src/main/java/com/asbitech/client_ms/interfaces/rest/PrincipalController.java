package com.asbitech.client_ms.interfaces.rest;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.asbitech.client_ms.application.ClientCommandManager;
import com.asbitech.client_ms.commands.ActivateAccountCommand;
import com.asbitech.client_ms.commands.AuthenticateUserCommand;
import com.asbitech.client_ms.commands.CreatePrincipalCommand;
import com.asbitech.client_ms.commands.EnableMfaByTokenCommand;
import com.asbitech.client_ms.commands.UpdateProfileCommand;
import com.asbitech.client_ms.commands.ValidateOtpCommand;
import com.asbitech.client_ms.domain.entity.PrincipalAggregateRoot;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.infra.external.keycloak.dto.AuthRequest;
import com.asbitech.client_ms.interfaces.dto.ActivateMfaRequest;
import com.asbitech.client_ms.interfaces.dto.ActivatePrincipalRequest;
import com.asbitech.client_ms.interfaces.dto.CreatePrincipalRequest;
import com.asbitech.client_ms.interfaces.dto.SettingsRequest;
import com.asbitech.client_ms.interfaces.dto.ValidateOtpRequest;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/principals")
public class PrincipalController {
    

    private static final Logger LOG = LoggerFactory.getLogger(PrincipalController.class);

    private final ClientCommandManager clientCommandManager;
    private final ClientRepository clientRepository;
    private static final String USER_ID_HEADER = "X-User-ID";

    public PrincipalController(ClientCommandManager clientCommandManager, ClientRepository clientRepository) {
        this.clientCommandManager = clientCommandManager;
        this.clientRepository =clientRepository;

    }

    @PostMapping(path = "/public/activate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> activatePrincipal(@RequestBody ActivatePrincipalRequest request) {
        return clientCommandManager.processCommand(ActivateAccountCommand.builder()
                                                .activationToken(request.activationToken())
                                                .newPassword(request.newPassword())
                                                .tempPassword(request.tempPassword())
                                                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED) // 201 Created
                        .body(response.get()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }

    @PostMapping(path = "/public/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> authUser(@RequestBody AuthRequest request) {
        return clientCommandManager.processCommand(AuthenticateUserCommand.builder()
                                                .username(request.username())
                                                .password(request.password())
                                                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.OK) // 201 Created
                        .body(response.get().getUserCredential().getAuthResponse()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }

    @PostMapping(path = "/public/activateMfaByToken", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> activateMfaByMfaToken(@RequestBody ActivateMfaRequest request) {
        return clientCommandManager.processCommand(EnableMfaByTokenCommand.builder()
                                                .activationToken(request.activationToken())
                                                .channel(request.channel())
                                                .phoneNumber(request.phoneNumber())
                                                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.ACCEPTED).build()); // 202 Accepted
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }


    @PostMapping(path = "/public/activateMfaByToken/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> confirmMfaActivationByMfaToken(@RequestBody ValidateOtpRequest request) {
        return clientCommandManager.processCommand(ValidateOtpCommand.builder()
                                                .activationToken(request.activationToken())
                                                .otp(request.otp())
                                                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.NO_CONTENT).build()); // 202 Accepted
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }


    @GetMapping(path = "/public/profileByActivationToken", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getUserProfile(@RequestParam String activationToken) {
        LOG.info("getUserProfile activationToken: {}", activationToken);
        return clientRepository.loadByActivationtoken(activationToken)
                    .flatMap(response -> {
                        if (response.isRight()) {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.OK) 
                                    .body(response.get()));
                        } else {
                            CustomError customError = response.getLeft();
                            LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                            LOG.error("Error creating principal: {}", customError.getMessage());

                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.UNAUTHORIZED) // Return actual error status
                                    .body(customError)); // Return CustomError as response body
                        }
                    });
    }


    @PostMapping(path = "/public", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> createPrincipal(@RequestBody CreatePrincipalRequest request) {
        return clientCommandManager.processCommand(CreatePrincipalCommand.builder()
                                                .alias(request.alias())
                                                .mail(request.mail())
                                                .mainPlatformId(request.mainPlatformId())
                                                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED) // 201 Created
                        .body(response.get()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }

    @GetMapping(path = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getUserProfileByToken(@RequestHeader(USER_ID_HEADER) String userId) {
        LOG.info(userId); 
        return clientRepository.loadByIamUserId(userId)
                    .flatMap(response -> {
                        if (response.isRight()) {
                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.OK) 
                                    .body(response.get()));
                        } else {
                            CustomError customError = response.getLeft();
                            LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                            LOG.error("Error creating principal: {}", customError.getMessage());

                            return Mono.just(ResponseEntity
                                    .status(HttpStatus.UNAUTHORIZED) // Return actual error status
                                    .body(customError)); // Return CustomError as response body
                        }
                    });
    }


    @PutMapping(path = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> updateSettings(@RequestHeader(USER_ID_HEADER) String userId, @RequestBody SettingsRequest request) {
        return clientCommandManager.processCommand(UpdateProfileCommand.builder()
                .userId(userId)
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .organizationName(request.organizationName())
                .fullName(request.fullName())
                .build()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED) // 201 Created
                        .body(response.get()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating principal: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }
}
