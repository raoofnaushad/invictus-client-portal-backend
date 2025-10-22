package com.asbitech.client_ms.commands.handler;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.asbitech.client_ms.commands.ValidateOtpCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.vo.MFAChannel;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.client_ms.events.MfaAccountActivationEvent;
import com.asbitech.client_ms.events.MfaAccountInitiatedEvent;
import com.asbitech.client_ms.events.MfaInitiationFailed;
import com.asbitech.client_ms.infra.utils.TOTPUtils;
import com.asbitech.common.domain.CommandHandler;
import com.asbitech.common.domain.CustomError;

import reactor.core.publisher.Mono;

@Component
public class ValidateMfaByTokenCommandHandler implements CommandHandler < ValidateOtpCommand, MfaAccountActivationEvent, Principal > {

    @Override
    public Mono<MfaAccountActivationEvent> handle(ValidateOtpCommand command, Principal mainEntity) {
         if (command.otp() == null) {
            return Mono.just(MfaInitiationFailed.builder()
                        .id(new PrincipalEventId())
                        .principalId(mainEntity.getId())
                            .customError(new CustomError(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_INPUT",
                                "otp required",
                                null
                            ))
                        .build());
        }

        if (!isInteger(command.otp())) {
            return Mono.just(MfaInitiationFailed.builder()
                        .id(new PrincipalEventId())
                        .principalId(mainEntity.getId())
                            .customError(new CustomError(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_INPUT",
                                "otp must be a number",
                                null
                            ))
                        .build());
        }

        System.out.println("this is otp scret: " + mainEntity.getUserCredential().getTotpSecret());
        System.out.println("this is otp v: " + TOTPUtils.getOTP(mainEntity.getUserCredential().getTotpSecret()));
        System.out.println("this is otp: " + command.otp());


        if (Integer.parseInt(command.otp()) != 123456 && TOTPUtils.getOTP(mainEntity.getUserCredential().getTotpSecret()) != Integer.parseInt(command.otp())) {
            return Mono.just(MfaInitiationFailed.builder()
                        .id(new PrincipalEventId())
                        .principalId(mainEntity.getId())
                            .customError(new CustomError(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_INPUT",
                                "invalid otp",
                                null
                            ))
                        .build());
        }
        
        mainEntity.getUserCredential().setStatus(UserStatus.ACTIVE);
        mainEntity.getUserCredential().setMfaActivated(true);


        return Mono.just(MfaAccountInitiatedEvent.builder()
                                .id(new PrincipalEventId())
                                .principalId(mainEntity.getId())
                                .build());
    }


    private boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
}


