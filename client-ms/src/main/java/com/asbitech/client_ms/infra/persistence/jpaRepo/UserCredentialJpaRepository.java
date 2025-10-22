package com.asbitech.client_ms.infra.persistence.jpaRepo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.asbitech.client_ms.infra.persistence.table.UserCredentialTable;

import reactor.core.publisher.Mono;


@Repository
public interface UserCredentialJpaRepository extends ReactiveCrudRepository<UserCredentialTable, String> {
    Mono<UserCredentialTable> findByIamUserId(String iamUserId);
    Mono<UserCredentialTable> findByActivationToken(String activationToken);
}
