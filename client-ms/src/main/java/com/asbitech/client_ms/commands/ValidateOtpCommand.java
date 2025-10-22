package com.asbitech.client_ms.commands;

import java.security.Principal;

import com.asbitech.client_ms.domain.vo.ClientCommandType;
import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CommandType;

import lombok.Builder;

@Builder
public record ValidateOtpCommand(String otp, String activationToken) implements Command {
    @Override
    public CommandType getCommandType() {
        // TODO Auto-generated method stub
       return ClientCommandType.VALIDATE_OTP;
    }
    

}
