package com.asbitech.client_ms.application.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.asbitech.client_ms.application.ClientCommandManager;
import com.asbitech.client_ms.commands.ActivateAccountCommand;
import com.asbitech.client_ms.commands.EnableMfaByTokenCommand;
import com.asbitech.client_ms.commands.UpdateProfileCommand;
import com.asbitech.client_ms.commands.ValidateOtpCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.entity.PrincipalAggregateRoot;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.FailEvent;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;


@Service
public class ClientCommandManagerImpl implements ClientCommandManager {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCommandManagerImpl.class);
    private final ApplicationContext applicationContext;
    private final ClientRepository clientRepository;

    ClientCommandManagerImpl(ApplicationContext applicationContext, ClientRepository clientRepository) {
        this.applicationContext = applicationContext;
        this.clientRepository = clientRepository;
    }


    @Override
    public Mono<Either<CustomError, Principal>> processCommand(Command command) {
        return loadAggregateRoot(command).flatMap(principalAggregateRootRsp -> principalAggregateRootRsp.fold(error -> Mono.just(Either.left(error)), 
                    principalAggregateRoot -> principalAggregateRoot.handle(command)
                    .flatMap(response -> {
                        if (response instanceof FailEvent failEvent) {
                            LOG.error("Command failed: {}", failEvent);
                            return Mono.just(Either.left(failEvent.getCustomError()));
                        }

                        if (command.getCommandType().equals(ClientCommandType.AUTHENTICATE_USER)) {
                            return  Mono.just(Either.right(principalAggregateRoot.entity));
                        }

                        return clientRepository.store(principalAggregateRoot.entity)
                                .map(storeRsp -> {
                                    if (storeRsp.isRight()) {
                                        return Either.right(principalAggregateRoot.entity);
                                    } else {
                                        LOG.error("Error storing entity: {}", storeRsp.getLeft().getMessage());
                                        return Either.left(storeRsp.getLeft());
                                    }
                                });
                    })));
    }


    private Mono<Either<CustomError, PrincipalAggregateRoot>> loadAggregateRoot(Command command) {
        if (command.getCommandType().equals(ClientCommandType.CREATE_PRINCIPAL)) {
            return Mono.just(Either.right(new PrincipalAggregateRoot(applicationContext, Principal.builder().isNew(true).build())));
        }

        if (command.getCommandType().equals(ClientCommandType.AUTHENTICATE_USER)) {
            return Mono.just(Either.right(new PrincipalAggregateRoot(applicationContext, Principal.builder().isNew(false).build())));
        }

        if (command.getCommandType().equals(ClientCommandType.VALIDATE_OTP)) {
            return clientRepository.loadByActivationtoken(((ValidateOtpCommand)command).activationToken())
                    .map(response -> response.fold(
                            Either::left,
                            principal -> Either.right(new PrincipalAggregateRoot(applicationContext, principal))
                        ));
        }

        if (command.getCommandType().equals(ClientCommandType.ACTIVATE_ACCOUNT)) {
            return clientRepository.loadByActivationtoken(((ActivateAccountCommand)command).activationToken())
                    .map(response -> response.fold(
                            Either::left,
                            principal -> Either.right(new PrincipalAggregateRoot(applicationContext, principal))
                        ));
        }

        if (command.getCommandType().equals(ClientCommandType.ACTIVATE_MFA)) {
            return clientRepository.loadByActivationtoken(((EnableMfaByTokenCommand)command).activationToken())
                    .map(response -> response.fold(
                            Either::left,
                            principal -> Either.right(new PrincipalAggregateRoot(applicationContext, principal))
                        ));
        }

        if (command.getCommandType().equals(ClientCommandType.UPDATE_PROFILE)) {
            return clientRepository.loadByIamUserId(((UpdateProfileCommand)command).userId())
                    .map(response -> response.fold(
                            Either::left,
                            principal -> Either.right(new PrincipalAggregateRoot(applicationContext, principal))
                        ));
        }

        return Mono.just(Either.left(CustomError.builder()
                .message("Command not supported")
                .httpStatusCode(HttpStatus.BAD_REQUEST)
                .build()));
    }


    
}
