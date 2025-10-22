package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.EventType;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class AuthenticateUserFailedEvent implements AuthenticateUserEvent, FailEvent {
    PrincipalEventId id;
    String userName;
    CustomError customError;


    @Override
    public EventType getEventType() {
        return PrincipalEvent.LOGIN_FAILED;
    }
    
}
