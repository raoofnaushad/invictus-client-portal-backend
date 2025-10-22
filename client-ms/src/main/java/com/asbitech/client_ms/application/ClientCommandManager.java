package com.asbitech.client_ms.application;

import com.asbitech.client_ms.domain.entity.Principal;
import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface ClientCommandManager {
    // Process command and return the updated state
    Mono<Either<CustomError, Principal>> processCommand(Command command);

}
