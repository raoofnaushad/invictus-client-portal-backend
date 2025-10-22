package com.asbitech.client_ms.infra.mapper;

import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.client_ms.domain.entity.UserCredential;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.client_ms.domain.vo.UserCredentialId;
import com.asbitech.client_ms.domain.vo.UserStatus;
import com.asbitech.client_ms.infra.persistence.table.PrincipalTable;
import com.asbitech.client_ms.infra.persistence.table.UserCredentialTable;

public class PrincipalMapper {

        public static PrincipalTable toTable(Principal principal) {
            if (principal == null) {
                return null;
            }
    
            PrincipalTable table = new PrincipalTable();
            table.setId(principal.getId().toString()); // Assuming PrincipalId is a custom type
            table.setAlias(principal.getAlias());
            table.setCreatedAt(principal.getCreatedAt());
            table.setUpdatedAt(principal.getUpdatedAt());

            table.setIsNew(principal.getIsNew());

            return table;
        }

        public static Principal toDomain(PrincipalTable principalTable) {
            if (principalTable == null) {
                return null;
            }
    
            return Principal.builder()
                    .alias(principalTable.getAlias())
                    .id(new PrincipalId(principalTable.getId()))
                    .isNew(false)
                    .createdAt(principalTable.getCreatedAt())
                    .updatedAt(principalTable.getUpdatedAt())
                    .build();
        }

        // Mapping from UserCredential (Domain) to UserCredentialTable (Database)
        public  static UserCredentialTable toTable(UserCredential userCredential) {
            if (userCredential == null) {
                return null;
            }
    
            UserCredentialTable table = new UserCredentialTable();
            table.setId(userCredential.getId().toString());
            table.setFullName(userCredential.getFullName());
            table.setUsername(userCredential.getUsername());
            table.setIamUserId(userCredential.getIamUserId());
            table.setMfaActivated(userCredential.getMfaActivated());
            table.setMfaChannel(userCredential.getMfaChannel());
            table.setPhoneNumber(userCredential.getPhoneNumber());
            table.setStatus(userCredential.getStatus().name());
            table.setRefreshToken(userCredential.getRefreshToken());
            table.setTotpSecret(userCredential.getTotpSecret());
            table.setActivationToken(userCredential.getActivationToken()); 
            table.setActivationTokenExpiry(userCredential.getActivationTokenExpiry());   
            table.setNew(userCredential.getIsNew());
            return table;
        }
    
        // Mapping from UserCredentialTable (Database) to UserCredential (Domain)
        public static UserCredential toDomain(UserCredentialTable userCredentialTable) {
            if (userCredentialTable == null) {
                return null;
            }
    
            return UserCredential.builder()
                    .id(new UserCredentialId(userCredentialTable.getId())) // Assuming UserCredentialId has a constructor that takes a String
                    .username(userCredentialTable.getUsername())
                    .iamUserId(userCredentialTable.getIamUserId())
                    .phoneNumber(userCredentialTable.getPhoneNumber())
                    .fullName(userCredentialTable.getFullName())
                    .mfaActivated(userCredentialTable.getMfaActivated())
                    .activationToken(userCredentialTable.getActivationToken())
                    .activationTokenExpiry(userCredentialTable.getActivationTokenExpiry())
                    .mfaChannel(userCredentialTable.getMfaChannel())
                    .status(UserStatus.valueOf(userCredentialTable.getStatus()))
                    .refreshToken(userCredentialTable.getRefreshToken())
                    .totpSecret(userCredentialTable.getTotpSecret())
                    .isNew(false)
                    .build();
        }
}
