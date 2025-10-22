package com.asbitech.common.domain;


import reactor.core.publisher.Mono;

public interface CommandHandler<C extends Command, E extends Event, EN extends Entity<? extends EntityId>> {
    Mono<E> handle(C command, EN mainEntity);
}
