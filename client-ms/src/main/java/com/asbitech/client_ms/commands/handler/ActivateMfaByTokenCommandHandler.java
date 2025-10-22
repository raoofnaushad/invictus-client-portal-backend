package com.asbitech.client_ms.commands.handler;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.asbitech.client_ms.commands.EnableMfaByTokenCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.vo.MFAChannel;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.client_ms.events.AccountActivatedEvent;
import com.asbitech.client_ms.events.MfaAccountActivationEvent;
import com.asbitech.client_ms.events.MfaAccountInitiatedEvent;
import com.asbitech.client_ms.events.MfaInitiationFailed;
import com.asbitech.client_ms.infra.utils.TOTPUtils;
import com.asbitech.common.domain.CommandHandler;
import com.asbitech.common.domain.CustomError;

import reactor.core.publisher.Mono;

@Component
public class ActivateMfaByTokenCommandHandler implements CommandHandler < EnableMfaByTokenCommand, MfaAccountActivationEvent, Principal > {

    @Override
    public Mono<MfaAccountActivationEvent> handle(EnableMfaByTokenCommand command, Principal mainEntity) {
        if (command.channel() == null) {
            return Mono.just(MfaInitiationFailed.builder()
                        .id(new PrincipalEventId())
                        .principalId(mainEntity.getId())
                            .customError(new CustomError(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_INPUT",
                                "channel required",
                                null
                            ))
                        .build());
        }

        if (command.phoneNumber() == null && MFAChannel.valueOf(command.channel().toUpperCase()).equals(MFAChannel.SMS)) {
            return Mono.just(MfaInitiationFailed.builder()
                        .id(new PrincipalEventId())
                        .principalId(mainEntity.getId())
                            .customError(new CustomError(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_INPUT",
                                "phone number required",
                                null
                            ))
                        .build());
        }

        if ( MFAChannel.valueOf(command.channel().toUpperCase()).equals(MFAChannel.SMS)) {
            mainEntity.getUserCredential().setPhoneNumber(command.phoneNumber());
        }

    
        mainEntity.getUserCredential().setStatus(UserStatus.PENDING_MFA_VERIFICATION);
        mainEntity.getUserCredential().setMfaChannel(MFAChannel.valueOf(command.channel().toUpperCase()));
        

        if ( mainEntity.getUserCredential().getTotpSecret() == null) {
            mainEntity.getUserCredential().setTotpSecret(TOTPUtils.generateSecretKey());
        }

        mainEntity.getUserCredential().setTotpSecret(mainEntity.getUserCredential().getTotpSecret());

        System.out.println("this is otp scret: " + mainEntity.getUserCredential().getTotpSecret());

        System.out.println("this is otp: " + TOTPUtils.getOTP(mainEntity.getUserCredential().getTotpSecret()));

        
        return Mono.just(MfaAccountInitiatedEvent.builder()
                                .id(new PrincipalEventId())
                                .principalId(mainEntity.getId())
                                .build());
    }
    
}
