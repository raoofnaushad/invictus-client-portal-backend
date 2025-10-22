package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AccountActivationFailedEvent implements AccountActivationEvent, FailEvent {
    PrincipalEventId id;
    PrincipalId principalId;
    CustomError customError;

    @Override
    public PrincipalEvent getEventType() {
        return PrincipalEvent.ACCOUNT_ACTIVATION_FAILED;
    }
}
