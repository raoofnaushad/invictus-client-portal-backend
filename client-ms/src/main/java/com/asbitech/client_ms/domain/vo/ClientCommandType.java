package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.CommandType;

public enum ClientCommandType implements  CommandType {
    CREATE_PRINCIPAL,
    ACTIVATE_ACCOUNT,
    ACTIVATE_MFA,
    AUTHENTICATE_USER,
    VALIDATE_OTP,
    UPDATE_PROFILE,
}
