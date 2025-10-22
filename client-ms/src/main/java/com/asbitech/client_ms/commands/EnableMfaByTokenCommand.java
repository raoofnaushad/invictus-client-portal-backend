package com.asbitech.client_ms.commands;

import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.client_ms.domain.vo.MFAChannel;
import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CommandType;

import lombok.Builder;

@Builder
public record EnableMfaByTokenCommand(String phoneNumber, String channel, String activationToken) implements Command {
 @Override
    public CommandType getCommandType() {
       return ClientCommandType.ACTIVATE_MFA;
    }
}
