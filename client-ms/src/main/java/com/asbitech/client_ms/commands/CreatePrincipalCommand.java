package com.asbitech.client_ms.commands;

import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.common.domain.Command;

import lombok.Builder;


@Builder
public record CreatePrincipalCommand(String alias, String mainPlatformId, String mail) implements Command {

    @Override
    public ClientCommandType getCommandType() {
       return ClientCommandType.CREATE_PRINCIPAL;
    }   
       
}
