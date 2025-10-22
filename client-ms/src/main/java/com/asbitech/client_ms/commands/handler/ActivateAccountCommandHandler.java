package com.asbitech.client_ms.commands.handler;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


import com.asbitech.client_ms.commands.ActivateAccountCommand;
import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.repository.ClientRepository;
import com.asbitech.client_ms.domain.vo.PrincipalEventId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.client_ms.events.AccountActivatedEvent;
import com.asbitech.client_ms.events.AccountActivationEvent;
import com.asbitech.client_ms.events.AccountActivationFailedEvent;
import com.asbitech.client_ms.infra.utils.PasswordUtils;
import com.asbitech.common.domain.CommandHandler;

@Component
public class ActivateAccountCommandHandler implements CommandHandler < ActivateAccountCommand, AccountActivationEvent, Principal > {
    private final ClientRepository clientRepository;

    public ActivateAccountCommandHandler(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    @Override
    public Mono<AccountActivationEvent> handle(ActivateAccountCommand command, Principal mainEntity) {
        System.out.println("command " + command + "  mainEntity " + mainEntity.getUserCredential().getUsername());

        return clientRepository.authenticateUser(mainEntity.getUserCredential().getUsername(),
            command.tempPassword()).flatMap(response -> response.fold(
            error -> {
                System.out.println("error " + error);

                if (error.getHttpStatusCode().value() == 400) {
                    // TODO check that the error is relater to tem password
                    return clientRepository.updatePassword(mainEntity.getUserCredential().getIamUserId(), command.newPassword())
                        .flatMap(updateResponse -> updateResponse.fold(
                            errorUpdateRsp -> Mono.just(AccountActivationFailedEvent.builder()
                                .id(new PrincipalEventId())
                                .principalId(mainEntity.getId())
                                .customError(updateResponse.getLeft())
                                .build()),
                            successUpdateRsp -> {
                                mainEntity.getUserCredential().setActivationToken(PasswordUtils.generateTempPassword(30)); // token for mfa
                                mainEntity.getUserCredential().setActivationTokenExpiry(LocalDateTime.now().plusMinutes(30));
                                mainEntity.getUserCredential().setStatus(UserStatus.PENDING_MFA_ACTIVATION);


                                return Mono.just(AccountActivatedEvent.builder()
                                    .id(new PrincipalEventId())
                                    .principalId(mainEntity.getId())
                                    .build());

                            }));
                }
                // Handle the left side of the Either (error)
                return Mono.just(AccountActivationFailedEvent.builder()
                    .id(new PrincipalEventId())
                    .principalId(mainEntity.getId())
                    .customError(error)
                    .build());
            },
            tokenResponse -> {
                System.out.println("tokenResponse " + tokenResponse);

                return clientRepository.updatePassword(mainEntity.getUserCredential().getIamUserId(), command.newPassword())
                    .flatMap(updateResponse -> updateResponse.fold(
                        errorUpdateRsp ->  Mono.just(AccountActivationFailedEvent.builder()
                                .id(new PrincipalEventId())
                                .principalId(mainEntity.getId())
                                .customError(updateResponse.getLeft())
                                .build())
                        ,
                        successUpdateRsp -> {
                            mainEntity.getUserCredential().setActivationToken(null);
                            mainEntity.getUserCredential().setActivationTokenExpiry(null);
                            mainEntity.getUserCredential().setStatus(UserStatus.ACTIVE);

                            return Mono.just(AccountActivatedEvent.builder()
                                .id(new PrincipalEventId())
                                .principalId(mainEntity.getId())
                                .build());
                        }));
            }
        ));
    }

}