package com.asbitech.document_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class DocumentId extends EntityId {

    public DocumentId() {
        super();
    }

    public DocumentId(String id) {
        super(id);
    }

    @Override
    public String getPrefix() {
        return "doc-%s";
    }

}
