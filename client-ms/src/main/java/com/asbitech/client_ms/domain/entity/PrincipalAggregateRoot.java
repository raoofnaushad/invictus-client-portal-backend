package com.asbitech.client_ms.domain.entity;

import org.springframework.context.ApplicationContext;

import com.asbitech.client_ms.commands.ActivateAccountCommand;
import com.asbitech.client_ms.commands.AuthenticateUserCommand;
import com.asbitech.client_ms.commands.CreatePrincipalCommand;
import com.asbitech.client_ms.commands.EnableMfaByTokenCommand;
import com.asbitech.client_ms.commands.UpdateProfileCommand;
import com.asbitech.client_ms.commands.ValidateOtpCommand;
import com.asbitech.client_ms.commands.handler.ActivateAccountCommandHandler;
import com.asbitech.client_ms.commands.handler.ActivateMfaByTokenCommandHandler;
import com.asbitech.client_ms.commands.handler.AuthenticateUserCommandHandler;
import com.asbitech.client_ms.commands.handler.CreatePrincipalCommandHandler;
import com.asbitech.client_ms.commands.handler.UpdateProfileCommandHandler;
import com.asbitech.client_ms.commands.handler.ValidateMfaByTokenCommandHandler;
import com.asbitech.common.domain.AggregateRoot;

import lombok.Setter;

@Setter
public class PrincipalAggregateRoot extends AggregateRoot<Principal> {
    Principal domainEntity;


	public PrincipalAggregateRoot(ApplicationContext applicationContext, Principal domainEntity) {
		super(applicationContext, domainEntity);
	}

	@Override
    protected AggregateRootBehavior initialBehavior() {
        AggregateRootBehaviorBuilder behaviorBuilder = new AggregateRootBehaviorBuilder();
        behaviorBuilder.setCommandHandler(CreatePrincipalCommand.class, getHandler(CreatePrincipalCommandHandler.class));
        behaviorBuilder.setCommandHandler(ActivateAccountCommand.class, getHandler(ActivateAccountCommandHandler.class));
        behaviorBuilder.setCommandHandler(AuthenticateUserCommand.class, getHandler(AuthenticateUserCommandHandler.class));
        behaviorBuilder.setCommandHandler(ValidateOtpCommand.class, getHandler(ValidateMfaByTokenCommandHandler.class));
        behaviorBuilder.setCommandHandler(EnableMfaByTokenCommand.class, getHandler(ActivateMfaByTokenCommandHandler.class));
        behaviorBuilder.setCommandHandler(UpdateProfileCommand.class, getHandler(UpdateProfileCommandHandler.class));

        return behaviorBuilder.build();
    }
}
