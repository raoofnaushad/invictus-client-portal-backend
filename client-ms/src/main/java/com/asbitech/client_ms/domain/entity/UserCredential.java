package com.asbitech.client_ms.domain.entity;

import java.time.LocalDateTime;

import com.asbitech.client_ms.domain.vo.AuthResponse;
import com.asbitech.client_ms.domain.vo.MFAChannel;
import com.asbitech.client_ms.domain.vo.UserCredentialId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.common.domain.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@Getter
@Setter
@ToString
public class UserCredential extends Entity<UserCredentialId> {
    private String username;
    private String fullName;
    private String phoneNumber;
    private String iamUserId;
    private UserStatus status;
    private String activationToken;
    private LocalDateTime activationTokenExpiry;
    private Boolean mfaActivated;
    private MFAChannel mfaChannel;
    private String totpSecret;
    private String refreshToken;
    private Boolean isNew;
    private AuthResponse authResponse;
}
