package com.asbitech.client_ms.commands;

import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.common.domain.Command;

import lombok.Builder;

@Builder
public record ActivateAccountCommand (String activationToken, String tempPassword, String newPassword) implements Command {

    @Override
    public ClientCommandType getCommandType() {
       return ClientCommandType.ACTIVATE_ACCOUNT;
    }  

}
