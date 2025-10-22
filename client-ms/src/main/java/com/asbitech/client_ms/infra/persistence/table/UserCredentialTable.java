package com.asbitech.client_ms.infra.persistence.table;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import com.asbitech.client_ms.domain.vo.MFAChannel;

import lombok.Data;


@Data
@Table(name = "user_credential_table")
public class UserCredentialTable implements Persistable<String> {
    @Id
    private String id;

    private String username;

    private String fullName;

    private String phoneNumber;

    private String iamUserId;

    private String activationToken;
    
    private LocalDateTime activationTokenExpiry;

    private String refreshToken;

    private String totpSecret;

    private String status;

    private Boolean mfaActivated;

    private MFAChannel mfaChannel;

    @Transient
    private boolean isNew;

    @Override
    public boolean isNew() {
        return this.isNew;
    }
}
