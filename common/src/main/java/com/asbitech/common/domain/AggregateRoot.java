package com.asbitech.common.domain;

import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import io.vavr.control.Either; // Import Either from Vavr library
import reactor.core.publisher.Mono;

public abstract class AggregateRoot<E extends Entity<? extends EntityId>> {

    public final E entity;
    private final ApplicationContext applicationContext;
    private AggregateRootBehavior behavior;

    protected AggregateRoot(ApplicationContext applicationContext, E entity) {
        this.entity = entity;
        this.applicationContext = applicationContext;
        this.behavior = initialBehavior();
    }

    public <A extends Command, B extends Event> Mono<B> handle(A command) {
        CommandHandler<A, B, E> commandHandler = ( CommandHandler<A, B, E>) behavior.handlers.get(command.getClass());
        return commandHandler.handle(command, this.entity);
    }

    protected <A extends Command, B extends Event> CommandHandler<A, B, E> getHandler(Class<? extends CommandHandler> commandHandlerClass) {
        return applicationContext.getBean(commandHandlerClass);
    }

    protected abstract AggregateRootBehavior initialBehavior();


    public class AggregateRootBehavior {

        protected final Map<Class<? extends Command>, CommandHandler<? extends Command, ? extends Event, E>> handlers;

        public AggregateRootBehavior(Map<Class<? extends Command>, CommandHandler<? extends Command, ? extends Event, E>> handlers) {
            this.handlers = Collections.unmodifiableMap(handlers);
        }
    }

    public class AggregateRootBehaviorBuilder {

        private final Map<Class<? extends Command>, CommandHandler<? extends Command, ? extends Event, E>> handlers = new HashMap<>();

        public <A extends Command, B extends Event> AggregateRootBehaviorBuilder setCommandHandler(Class<A> commandClass, CommandHandler<A, B, E> handler) {
            handlers.put(commandClass, handler);
            return this;
        }

        public AggregateRootBehavior build() {
            return new AggregateRootBehavior(handlers);
        }
    }
}
