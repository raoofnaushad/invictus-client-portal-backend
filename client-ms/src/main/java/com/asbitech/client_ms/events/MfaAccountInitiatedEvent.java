package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.common.domain.EventType;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class MfaAccountInitiatedEvent implements  MfaAccountActivationEvent  {
    PrincipalEventId id;
    PrincipalId principalId;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.MFA_ACTIVATION_INITIATED;
    }    
}
