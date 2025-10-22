package com.asbitech.document_ms.infra.persistence.jpaRepo;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.asbitech.document_ms.infra.persistence.table.DocumentTable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DocumentJpaRepo extends ReactiveCrudRepository<DocumentTable, String> {
    @Query("SELECT * FROM document_table WHERE client_id = :clientId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<DocumentTable> findByClientId(String clientId, long limit, long offset);

    @Query("SELECT COUNT(*) FROM document_table WHERE client_id = :clientId")
    Mono<Long> countByClientId(String clientId);

     Mono<DocumentTable> findByFilePath(String fileName);
}