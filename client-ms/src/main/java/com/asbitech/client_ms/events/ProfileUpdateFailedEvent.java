package com.asbitech.client_ms.events;

import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.EventType;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
public class ProfileUpdateFailedEvent implements  ProfileUpdateEvent, FailEvent {
    PrincipalEventId id;
    CustomError customError;

    @Override
    public EventType getEventType() {
        return PrincipalEvent.PROFILE_UPDATE_FAILED;
    }
    
}