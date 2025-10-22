package com.asbitech.integration_ms.infra.persistence.table;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
@Table("integration_table")
public class IntegrationTable implements Persistable<String> {
    
    @Id
    private String id; // The ID of the PrincipalEntity
    
    private String accessToken; // Alias for the PrincipalEntity : used for getting details from main platform
    
    //private String institutionName;

    private String principalId;
    
    // For R2DBC, we need to handle collections differently
    // One approachn is to store products as a JSON array in a single column
    @Column("products")
    private String productsJson; // Will store JSON array of products
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime createdAt;
    
    @Transient
    private Boolean isNew = false;
    
    // Helper methods to work with the products JSON
    public List<String> getProducts() {
        if (productsJson == null || productsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(productsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void setProducts(List<String> products) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.productsJson = mapper.writeValueAsString(products);
        } catch (Exception e) {
            this.productsJson = "[]";
        }
    }
    
    @Override
    public boolean isNew() {
        return this.isNew;
    }
}