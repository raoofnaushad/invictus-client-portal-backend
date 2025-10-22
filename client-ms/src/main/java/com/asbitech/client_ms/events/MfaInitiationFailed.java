package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.EventType;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class MfaInitiationFailed implements  MfaAccountActivationEvent, FailEvent {
    PrincipalEventId id;
    PrincipalId principalId;
    CustomError customError;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.MFA_ACTIVATION_INIT_FAILED;
    }
    
}
