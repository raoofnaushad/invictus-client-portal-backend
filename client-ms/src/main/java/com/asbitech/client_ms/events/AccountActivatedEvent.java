package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.PrincipalId;

import lombok.Builder;


@Builder
public class AccountActivatedEvent implements AccountActivationEvent {
    PrincipalEventId id;
    PrincipalId principalId;

    @Override
    public PrincipalEvent getEventType() {
        return PrincipalEvent.ACCOUNT_ACTIVATED;
    }
}
