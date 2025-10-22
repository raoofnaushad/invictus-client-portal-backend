package com.asbitech.document_ms.infra.persistence.table;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.w3c.dom.DocumentType;

import com.asbitech.document_ms.domain.vo.DocumentStatus;
import com.asbitech.document_ms.infra.utils.JsonUtils;

import lombok.Data;

@Data
@Table(name = "document_table")
public class DocumentTable implements Persistable<String> {

    @Id
    private String id;
    private String name;
    private String description;
    private String filePath;
    private DocumentType documentType;
    private DocumentStatus documentStatus;
    private String clientId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column("extracted_data")
    private String extractedDataJson; // Store as JSON string

    public Map<String, Object> getExtractedData() {
        if (extractedDataJson == null) {
            return null;
        }
        return JsonUtils.toMap(extractedDataJson);
    }

    public void setExtractedData(Map<String, Object> map) {
        if(map == null) {
            this.extractedDataJson = null;
            return;
        }
        this.extractedDataJson = JsonUtils.toJson(map);
    }

    @Transient
    private Boolean isNew;

    @Override
    public boolean isNew() {
        return this.isNew;
    }
}