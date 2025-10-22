package com.asbitech.client_ms.commands.handler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.asbitech.client_ms.commands.CreatePrincipalCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.client_ms.events.PrincipalCreatedEvent;
import com.asbitech.client_ms.events.PrincipalCreationEvent;
import com.asbitech.client_ms.events.PrincipalCreationFailedEvent;
import com.asbitech.common.domain.CommandHandler;

import reactor.core.publisher.Mono;


@Component
public class CreatePrincipalCommandHandler implements CommandHandler<CreatePrincipalCommand, PrincipalCreationEvent, Principal> {
    private final ClientRepository clientRepository;

    public CreatePrincipalCommandHandler(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Mono<PrincipalCreationEvent> handle(CreatePrincipalCommand command, Principal mainEntity) {
        return clientRepository.generateUserCredential(command.mail(), command.alias(), "test", command.mail())
                        .map(response -> response.fold(
                            error ->  PrincipalCreationFailedEvent.builder()
                                    .id(new PrincipalEventId())
                                    .command(command)
                                    .customError(error)
                                    .build(),
                            userCredential -> {
                                // Handle the right side of the Either (success)
                                mainEntity.setId(new PrincipalId());
                                mainEntity.setAlias(command.alias());
                                mainEntity.setCreatedAt(LocalDateTime.now());
                                mainEntity.setUpdatedAt(LocalDateTime.now());
                                mainEntity.setUserCredential(userCredential);

                                return PrincipalCreatedEvent.builder()
                                    .id(new PrincipalEventId())
                                    .principal(mainEntity)
                                    .build();
                            }
                        ));

    }
}
