package com.asbitech.common.domain;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@AllArgsConstructor
@Setter
@Getter@ToString
public abstract class Entity<ID extends EntityId> {
    ID id;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    EventType lasEventType;
    LocalDateTime lastEventAt;
}
