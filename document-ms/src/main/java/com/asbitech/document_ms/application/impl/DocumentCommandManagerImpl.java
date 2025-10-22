package com.asbitech.document_ms.application.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.asbitech.common.domain.Command;
import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.FailEvent;
import com.asbitech.document_ms.application.DocumentCommandManager;
import com.asbitech.document_ms.commands.DocumentCommandType;
import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.entity.DocumentAggregateRoot;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

@Service
public class DocumentCommandManagerImpl implements DocumentCommandManager {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentCommandManagerImpl.class);
    private final ApplicationContext applicationContext;

    DocumentCommandManagerImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Override
    public Mono<Either<CustomError, Document>> processCommand(Command command) {
        return loadAggregateRoot(command).flatMap(documentAggregateRootRsp -> 
                documentAggregateRootRsp.fold(
                    error -> Mono.just(Either.left(error)), 
                    documentAggregateRoot -> documentAggregateRoot.handle(command)
                        .flatMap(response -> {
                            if (response instanceof FailEvent failEvent) {
                                LOG.error("Command failed: {}", failEvent);
                                return Mono.just(Either.left(failEvent.getCustomError()));
                            }
                            return Mono.just(Either.right(documentAggregateRoot.entity));
                        })));
    }



    private Mono<Either<CustomError, DocumentAggregateRoot>> loadAggregateRoot(Command command) {
        if (command.getCommandType().equals(DocumentCommandType.UPLOAD_DOCUMENT)) {
            return Mono.just(Either.right(new DocumentAggregateRoot(applicationContext, Document.builder().isNew(true).build())));
        }

        return Mono.just(Either.left(CustomError.builder()
                .message("Command not supported")
                .httpStatusCode(HttpStatus.BAD_REQUEST)
                .build()));
    }
}
