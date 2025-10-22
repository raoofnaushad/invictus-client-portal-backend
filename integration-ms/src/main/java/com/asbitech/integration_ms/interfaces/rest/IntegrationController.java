package com.asbitech.integration_ms.interfaces.rest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asbitech.common.domain.CustomError;
import com.asbitech.integration_ms.infra.external.PlaidService;
import com.asbitech.integration_ms.infra.persistence.jpa.IntegrationTableJpa;
import com.asbitech.integration_ms.infra.persistence.table.IntegrationTable;
import com.asbitech.integration_ms.interfaces.dto.ExchangeTokenRequest;
import com.asbitech.integration_ms.interfaces.dto.LinkTokenRequest;
import com.plaid.client.model.Products;

import reactor.core.publisher.Mono;

@RestController
//@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/integrations")
public class IntegrationController {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationController.class);
    private static final String USER_ID_HEADER = "X-User-ID";

    private final PlaidService plaidService;
    // TODO refactor to clean architecture
    private final IntegrationTableJpa integrationTableJpa;

    public IntegrationController(PlaidService plaidService, IntegrationTableJpa integrationTableJpa) {
        this.integrationTableJpa = integrationTableJpa;
        this.plaidService = plaidService;
    }

    @PostMapping(path = "/plaid/link-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> generateLinkToken(@RequestHeader(USER_ID_HEADER) String userId, @RequestBody LinkTokenRequest request) {
        LOG.info(userId);


        return plaidService.createLinkToken(userId, request.type()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED) // 201 Created
                        .body(response.get()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating link token: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }

    @PostMapping(path = "/plaid/exchange-token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> exchangePublicToken(@RequestHeader(USER_ID_HEADER) String userId, @RequestBody ExchangeTokenRequest request) {
        LOG.info(userId);

        return plaidService.exchangePublicToken(request.publicToken()).flatMap(response -> {
            if (response.isRight()) {

                IntegrationTable integrationTable = new IntegrationTable();
                integrationTable.setId(UUID.randomUUID().toString());
                integrationTable.setAccessToken(response.get().getAccessToken());
                integrationTable.setPrincipalId(userId);
                integrationTable.setProducts(request.products());
                //integrationTable.setInstitutionName(request.institutionName());
                integrationTable.setCreatedAt(LocalDateTime.now());
                integrationTable.setUpdatedAt(LocalDateTime.now());
                integrationTable.setIsNew(true);

                integrationTableJpa.save(integrationTable).block(); // TODO: Handle this properly with reactive programming

                return Mono.just(ResponseEntity
                        .status(HttpStatus.ACCEPTED) // 201 Created
                        .body(response.get()));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating link token: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });
    }

    @GetMapping(path = "/plaid", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getPlaidIntegrations(@RequestHeader(USER_ID_HEADER) String userId) {
        LOG.info(userId);

        return integrationTableJpa.findAllByPrincipalIdOrderByCreatedAtDesc(userId).collectList().flatMap(response -> {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.OK) // 201 Created
                        .body(response));
            });
    }

}
