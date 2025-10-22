package com.asbitech.document_ms.domain.repository;


import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.PageResponse;
import com.asbitech.document_ms.domain.entity.Document;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface DocumentRepo {
    Mono<Either<CustomError, PageResponse<Document>>> loadByClientId(String clientId, int page, int size);
    Mono<Either<CustomError, Document>> loadById(String docId);
    Mono<Either<CustomError, Document>> store(Document doc);
    Mono<Either<CustomError, Document>> findByPath(String path);
}
