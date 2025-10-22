package com.asbitech.client_ms.commands;

import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CommandType;

import lombok.Builder;


@Builder
public record AuthenticateUserCommand(String username, String password) implements Command {

    @Override
    public CommandType getCommandType() {
        // TODO Auto-generated method stub
       return ClientCommandType.AUTHENTICATE_USER;
    }

}
