package com.asbitech.document_ms.domain.entity;


import java.util.Map;

import org.w3c.dom.DocumentType;

import com.asbitech.common.domain.Entity;
import com.asbitech.document_ms.domain.vo.DocumentId;
import com.asbitech.document_ms.domain.vo.DocumentStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@ToString
public class Document extends Entity<DocumentId> {
    private String name;
    private String description;
    private String filePath;
    private DocumentType documentType;
    private DocumentStatus documentStatus;
    private String clientId;
    private Map<String, Object> extractedData;
    private Boolean isNew;
}
