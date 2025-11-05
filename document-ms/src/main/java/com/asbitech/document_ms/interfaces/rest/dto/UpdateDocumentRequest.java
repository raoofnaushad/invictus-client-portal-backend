package com.asbitech.document_ms.interfaces.rest.dto;

import java.util.List;
import java.util.Map;

import com.asbitech.document_ms.domain.vo.DocumentStatus;

import lombok.Data;

@Data
public class UpdateDocumentRequest {
    private DocumentStatus documentStatus;
    private List<Map<String, Object>> extractedData;
}
