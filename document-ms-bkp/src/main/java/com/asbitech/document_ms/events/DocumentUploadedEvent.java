package com.asbitech.document_ms.events;

import java.time.Instant;

import com.asbitech.common.domain.EventType;
import com.asbitech.document_ms.domain.vo.DocumentId;

import lombok.Builder;

@Builder
public class DocumentUploadedEvent implements DocumentUploadEvent {
    DocumentId id;
    String uploadedByUserId;
    String filename;
    String storedPath;
    Instant uploadedAt;

    @Override
    public EventType getEventType() {
        return DocumentEventType.DOCUMENT_UPLOADED;
    }
}
