package com.asbitech.client_ms.events;

import com.asbitech.client_ms.commands.CreatePrincipalCommand;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.EventType;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class PrincipalCreationFailedEvent  implements PrincipalCreationEvent, FailEvent {
    PrincipalEventId id;
    CreatePrincipalCommand command;
    CustomError customError;


    @Override
    public EventType getEventType() {
        return PrincipalEvent.PRINCIPAL_CREATION_FAILED;
}
}
