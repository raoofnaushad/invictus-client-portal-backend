package com.asbitech.document_ms.commands;

import java.util.List;
import java.util.Map;

import com.asbitech.common.domain.Command;
import com.asbitech.document_ms.domain.vo.DocumentId;
import com.asbitech.document_ms.domain.vo.DocumentStatus;

import lombok.Builder;


@Builder
public record UpdateDocumentCommand(DocumentId documentId, List<Map<String, Object>> extractedDdata, DocumentStatus documentStatus) implements Command {
    @Override
    public DocumentCommandType getCommandType() {
       return DocumentCommandType.UPDATE_DOCUMENT;
    }  
}