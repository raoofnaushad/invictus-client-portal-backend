package com.asbitech.document_ms.infra.externel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtractRequest {
    @JsonProperty("file_path")
    private String filePath;
}
