package com.asbitech.client_ms.commands.handler;


import org.springframework.stereotype.Component;
import com.asbitech.client_ms.commands.UpdateProfileCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.events.ProfileUpdateEvent;
import com.asbitech.client_ms.events.ProfileUpdatedEvent;
import com.asbitech.common.domain.CommandHandler;

import reactor.core.publisher.Mono;


@Component
public class UpdateProfileCommandHandler implements CommandHandler<UpdateProfileCommand, ProfileUpdateEvent, Principal> {

    @Override
    public Mono<ProfileUpdateEvent> handle(UpdateProfileCommand command, Principal mainEntity) {
        if (command.organizationName() != null) {
            mainEntity.setAlias(command.organizationName());
        }

        if (command.fullName() != null) {
            mainEntity.getUserCredential().setFullName(command.fullName());
        }

        return Mono.just(ProfileUpdatedEvent.builder()
                .id(new PrincipalEventId())
                .build());

    }
}