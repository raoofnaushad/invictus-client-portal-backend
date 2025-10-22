package com.asbitech.client_ms.domain.vo;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED,
    PENDING_PASSWORD_RESET,
    PENDING_EMAIL_VERIFICATION,
    PENDING_MFA_ACTIVATION,
    PENDING_MFA_VERIFICATION
}
