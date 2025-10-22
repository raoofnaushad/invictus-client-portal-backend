package com.asbitech.document_ms.infra.mapper;

import java.time.LocalDateTime;

import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.vo.DocumentId;
import com.asbitech.document_ms.infra.persistence.table.DocumentTable;
import com.asbitech.document_ms.infra.utils.JsonUtils;

public class DocumentMapper {
        public static DocumentTable toTable(Document doc) {
            if (doc == null) {
                return null;
            }
            System.out.println("Mapping document: " + doc);

            DocumentTable table = new DocumentTable();
            table.setId(doc.getId().id);
            table.setName(doc.getName());
            table.setDescription(doc.getDescription());
            table.setFilePath(doc.getFilePath());
            table.setDocumentType(doc.getDocumentType());
            table.setDocumentStatus(doc.getDocumentStatus());
            table.setClientId(doc.getClientId());
            table.setCreatedAt(doc.getCreatedAt());
            table.setUpdatedAt(LocalDateTime.now());
            table.setExtractedData(doc.getExtractedData());

            System.out.println("Mapped to table: " + table);

            return table;
        }

        public static Document toDomain(DocumentTable documentTable) {
            if (documentTable == null) {
                return null;
            }
    
            return Document.builder()
                .id(new DocumentId(documentTable.getId()))
                .name(documentTable.getName())
                .description(documentTable.getDescription())
                .filePath(documentTable.getFilePath())
                .updatedAt(documentTable.getUpdatedAt())
                .createdAt(documentTable.getCreatedAt())
                .documentType(documentTable.getDocumentType())
                .documentStatus(documentTable.getDocumentStatus())
                .clientId(documentTable.getClientId())
                .extractedData(documentTable.getExtractedData())
                .isNew(false)
                .build();
        }
}
