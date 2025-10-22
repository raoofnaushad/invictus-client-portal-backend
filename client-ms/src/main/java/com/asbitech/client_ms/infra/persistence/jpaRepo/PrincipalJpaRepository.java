package com.asbitech.client_ms.infra.persistence.jpaRepo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.asbitech.client_ms.infra.persistence.table.PrincipalTable;

import reactor.core.publisher.Mono;


@Repository
public interface PrincipalJpaRepository extends ReactiveCrudRepository<PrincipalTable, String> {
    Mono<PrincipalTable> findByAlias(String alias);
    Mono<PrincipalTable> findByUserCredentialId(String userCredentialId);
}
