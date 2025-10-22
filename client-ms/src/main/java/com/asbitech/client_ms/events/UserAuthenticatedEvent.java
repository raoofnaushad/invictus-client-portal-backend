package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.common.domain.EventType;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class UserAuthenticatedEvent implements AuthenticateUserEvent {
    PrincipalEventId id;
    String userName;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.LOGIN_SUCCESS;
    }
    
}
