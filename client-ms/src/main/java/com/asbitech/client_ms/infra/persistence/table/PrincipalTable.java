package com.asbitech.client_ms.infra.persistence.table;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table(name = "principal_table")
public class PrincipalTable implements Persistable<String> {

    @Id
    private String id; // The ID of the PrincipalEntity

    private String alias; // Alias for the PrincipalEntity : used for getting details from main platform

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String userCredentialId;

    @Transient
    private Boolean isNew;

    @Override
    public boolean isNew() {
        return this.isNew;
    }
}
