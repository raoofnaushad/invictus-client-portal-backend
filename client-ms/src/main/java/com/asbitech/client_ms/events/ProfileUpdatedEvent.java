package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.common.domain.EventType;

import lombok.Builder;


@Builder
public class ProfileUpdatedEvent implements ProfileUpdateEvent {
    PrincipalEventId id;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.PROFILE_UPDATED;
}
}