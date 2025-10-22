package com.asbitech.client_ms.commands.handler;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import com.asbitech.client_ms.commands.AuthenticateUserCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.entity.UserCredential;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.domain.vo.AuthResponse;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;

import com.asbitech.client_ms.events.AuthenticateUserEvent;
import com.asbitech.client_ms.events.AuthenticateUserFailedEvent;
import com.asbitech.client_ms.events.UserAuthenticatedEvent;
import com.asbitech.common.domain.CommandHandler;

@Component
public class AuthenticateUserCommandHandler implements CommandHandler <AuthenticateUserCommand, AuthenticateUserEvent, Principal> {
    private final ClientRepository clientRepository;

    public AuthenticateUserCommandHandler(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    @Override
    public Mono<AuthenticateUserEvent> handle(AuthenticateUserCommand command, Principal mainEntity) {
        System.out.println("command " + command);

        
        return clientRepository.authenticateUser(command.username(),
            command.password()).flatMap(response -> response.fold(
            error -> {
                System.out.println("error " + error);

                return Mono.just(AuthenticateUserFailedEvent.builder()
                    .id(new PrincipalEventId())
                    .userName(null)
                    .customError(error)
                    .build());
            },
            tokenResponse -> {
                System.out.println("tokenResponse " + tokenResponse);
                mainEntity.setUserCredential(UserCredential.builder().authResponse(new AuthResponse(tokenResponse.access_token(), tokenResponse.expires_in(), tokenResponse.refresh_expires_in(), tokenResponse.refresh_token(), tokenResponse.token_type(), 0, null, tokenResponse.scope())).build());

                return Mono.just(UserAuthenticatedEvent.builder()
                    .id(new PrincipalEventId())
                    .build());
            }
        ));
    }

}