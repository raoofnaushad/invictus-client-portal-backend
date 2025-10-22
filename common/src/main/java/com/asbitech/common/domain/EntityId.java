package com.asbitech.common.domain;


import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonValue;

public abstract class EntityId {
    
    public final String id;

    public EntityId() {
        this.id = String.format(getPrefix(), UUID.randomUUID().toString());
    }

    public EntityId(String id) {
        this.id = id;
    }

    
    protected abstract String getPrefix();

    @JsonValue
    public String getId() {
        return id;
    }
    
    public String toString() {
        return id;
    }
}
