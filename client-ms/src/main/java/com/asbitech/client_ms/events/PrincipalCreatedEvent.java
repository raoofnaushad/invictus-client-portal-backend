package com.asbitech.client_ms.events;


import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;

import com.asbitech.common.domain.EventType;

import lombok.Builder;


@Builder
public class PrincipalCreatedEvent implements PrincipalCreationEvent {
    PrincipalEventId id;
    Principal principal;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.PRINCIPAL_CREATED;
}
}
