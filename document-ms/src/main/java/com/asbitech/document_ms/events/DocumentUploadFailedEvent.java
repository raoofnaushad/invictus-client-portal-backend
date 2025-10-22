package com.asbitech.document_ms.events;

import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.EventType;
import com.asbitech.common.domain.FailEvent;

import lombok.Builder;



@Builder
public class DocumentUploadFailedEvent implements DocumentUploadEvent , FailEvent {
    CustomError error;
    String uploadedByUserId;
    String filename;

    @Override
    public EventType getEventType() {
        return DocumentEventType.DOCUMENT_UPLOAD_FAILED;
    }

    @Override
    public CustomError getCustomError() {
        return error;
    }

}
