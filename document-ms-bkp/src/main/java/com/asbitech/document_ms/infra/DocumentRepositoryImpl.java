package com.asbitech.document_ms.infra;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.asbitech.common.domain.CustomError;
import com.asbitech.common.domain.PageResponse;
import com.asbitech.document_ms.domain.entity.Document;
import com.asbitech.document_ms.domain.repository.DocumentRepo;
import com.asbitech.document_ms.infra.mapper.DocumentMapper;
import com.asbitech.document_ms.infra.persistence.jpaRepo.DocumentJpaRepo;
import com.asbitech.document_ms.infra.persistence.table.DocumentTable;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;


@Repository
public class DocumentRepositoryImpl implements DocumentRepo {
    private final DocumentJpaRepo documentJpaRepo;

    public DocumentRepositoryImpl(DocumentJpaRepo documentJpaRepo) {
        this.documentJpaRepo = documentJpaRepo;
    }

    @Override
    public Mono<Either<CustomError, PageResponse<Document>>> loadByClientId(String clientId, int page, int size) {
        long offset = (long) page * size;

        Mono<List<Document>> docsMono = documentJpaRepo.findByClientId(clientId, size, offset)
            .map(DocumentMapper::toDomain)
            .collectList();

        Mono<Long> countMono = documentJpaRepo.countByClientId(clientId);

        return Mono.zip(docsMono, countMono)
            .map(tuple -> {
                List<Document> docs = tuple.getT1();
                long total = tuple.getT2();

                PageResponse<Document> response = new PageResponse<>(
                    docs,
                    page,
                    size,
                    total
                );

                return Either.<CustomError, PageResponse<Document>>right(response);
            })
            .onErrorResume(ex ->
                Mono.just(Either.left(new CustomError(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "DB_ERROR",
                                ex.getMessage(),
                                null
                            )))
            );
    }

    @Override
    public Mono<Either<CustomError, Document>> store(Document doc) {
        try {
        DocumentTable table = DocumentMapper.toTable(doc);

        System.out.println("Storing document: " + doc);


        // Définir si c'est un insert ou update
        if (doc.getIsNew()) {
            table.setIsNew(true);
        } else {
            table.setIsNew(false);
        }

        return documentJpaRepo.save(table)                  // reactive save
                         .map(DocumentMapper::toDomain) // entity -> domain
                         .map(Either::<CustomError, Document>right)
                         .onErrorResume(ex ->
                             Mono.just(Either.left(new CustomError(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "DB_ERROR",
                                ex.getMessage(),
                                null
                            )))
                         );
    } catch (Exception ex) {
        return Mono.just(Either.left(new CustomError(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "MAPPER_ERROR",
                                ex.getMessage(),
                                null
                            )));
    }
    }

    @Override
    public Mono<Either<CustomError, Document>> findByPath(String path) {
        System.out.println("Finding document by path: " + path);
        
        return documentJpaRepo.findByFilePath(path)
                        .map(DocumentMapper::toDomain)
                        .map(Either::<CustomError, Document>right)
                        .switchIfEmpty(Mono.just(Either.left(new CustomError(
                            HttpStatus.NOT_FOUND,
                            "NOT_FOUND",
                            "Document not found with path: " + path,
                            null
                        ))))
                        .onErrorResume(ex ->
                             Mono.just(Either.left(new CustomError(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "DB_ERROR",
                                ex.getMessage(),
                                null
                            )))
                         );
    }

    @Override
    public Mono<Either<CustomError, Document>> loadById(String docId) {
        return documentJpaRepo.findById(docId)
            .map(DocumentMapper::toDomain) // map to your domain object
            .map(Either::<CustomError, Document>right)
            .switchIfEmpty(Mono.just(Either.left(new CustomError(
                                        HttpStatus.NOT_FOUND,
                                        "ACTIVATION_TOKEN_NOT_FOUND",
                                        "No user credential found with the given activation token",
                                        null
                                    ))));
    }
    

}
