package com.asbitech.document_ms.events;

import com.asbitech.common.domain.EventType;


public enum DocumentEventType implements EventType {
    DOCUMENT_UPLOADED,
    DOCUMENT_UPLOAD_FAILED,
    DOCUMENT_DELETED,
    DOCUMENT_UPDATED;
}