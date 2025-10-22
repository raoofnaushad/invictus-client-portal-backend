package com.asbitech.document_ms.application;

import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CustomError;
import com.asbitech.document_ms.domain.entity.Document;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface DocumentCommandManager {
    Mono<Either<CustomError, Document>> processCommand(Command command);

}