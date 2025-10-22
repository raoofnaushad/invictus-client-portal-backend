package com.asbitech.integration_ms.infra.persistence.jpa;


import java.util.List;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.asbitech.integration_ms.infra.persistence.table.IntegrationTable;

import reactor.core.publisher.Flux;


@Repository
public interface IntegrationTableJpa extends ReactiveCrudRepository<IntegrationTable, String> {
    Flux<IntegrationTable> findAllByPrincipalIdOrderByCreatedAtDesc(String principalId);
}
