package com.asbitech.portfolio_ms.interfaces.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asbitech.common.domain.CustomError;
import com.asbitech.portfolio_ms.domain.entity.AccountAsset;
import com.asbitech.portfolio_ms.infra.external.IntegrationService;
import com.asbitech.portfolio_ms.infra.external.PlaidService;
import com.asbitech.portfolio_ms.infra.mapper.InvestmentHoldingMapper;
import com.asbitech.portfolio_ms.infra.mapper.LiabilityMapper;
import com.asbitech.portfolio_ms.infra.mapper.TransactionSyncMapper;
import com.asbitech.portfolio_ms.interfaces.dto.AccessTokenBody;
import com.plaid.client.model.InvestmentsHoldingsGetResponse;
import com.plaid.client.model.InvestmentsTransactionsGetResponse;
import com.plaid.client.model.LiabilitiesGetResponse;
import com.plaid.client.model.Products;
import com.plaid.client.model.TransactionsSyncResponse;

import io.vavr.control.Either;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
    private static final Logger LOG = LoggerFactory.getLogger(PortfolioController.class);
    private static final String USER_ID_HEADER = "X-User-ID";

    private final PlaidService plaidService;
    private final IntegrationService integrationService;

    public PortfolioController(PlaidService plaidService, IntegrationService integrationService) {
        this.plaidService = plaidService;
        this.integrationService = integrationService;
    }

    
    @GetMapping(path = "/{id}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getAccounts(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable("id") String id) {
        LOG.info("User ID: {}", userId);
    
        return integrationService.getUserIntegrations(userId).flatMap(integrationsEither -> {
            if (integrationsEither.isLeft()) {
                CustomError error = integrationsEither.getLeft();
                return Mono.just(ResponseEntity
                        .status(error.getHttpStatusCode())
                        .body(error.getArgs()));
            }
    
            List<Map<String, Object>> integrations = integrationsEither.get();
    
            List<Mono<TransactionsSyncResponse>> transactionMonos = integrations.stream()
                    .map(integration -> {
                        String accessToken = (String) integration.get("accessToken");
                        if (accessToken == null) return null;
    
                        LOG.info("Access Token: {}", accessToken);
    
                        return plaidService.getTransactions(accessToken, 100)
                                .filter(Either::isRight)
                                .map(Either::get)
                                    .onErrorResume(e -> {
                                    LOG.warn("Error fetching transactions: {}", e.getMessage());
                                    return Mono.empty();
                                });
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    
            return Flux.fromIterable(transactionMonos)
                    .flatMap(Function.identity())
                    .reduceWith(HashMap<String, AccountAsset>::new, (assetMap, transactions) -> {
                        TransactionSyncMapper.mapResponseToAccountAssets(transactions)
                                .forEach(asset -> assetMap.put(asset.getId().id, asset));
                        return assetMap;
                    })
                    .map(assetMap -> ResponseEntity.ok(new ArrayList<>(assetMap.values())));
        });
    }
    
        @GetMapping(path = "/{id}/investment-accounts", produces = MediaType.APPLICATION_JSON_VALUE)
        public Mono<ResponseEntity<?>> getInvestmentAccounts(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable("id") String id) {
            LOG.info("Processing request for user: {}", userId);        
            return integrationService.getUserIntegrations(userId).flatMap(integrationsEither -> {
                if (integrationsEither.isLeft()) {
                    CustomError error = integrationsEither.getLeft();
                    return Mono.just(ResponseEntity
                            .status(error.getHttpStatusCode())
                            .body(error.getArgs()));
                } else {
                    List<Map<String, Object>> integrations = integrationsEither.get();
                    
                    // Process each integration/access token individually
                    List<Mono<List<AccountAsset>>> assetsMonos = integrations.stream()
                        .filter(integration -> integration.get("accessToken") != null)
                        .map(integration -> {
                            String accessToken = (String)integration.get("accessToken");
                            LOG.info("Processing access token: {}", accessToken);
                            
                            // Get both holdings and transactions for this specific access token
                            Mono<InvestmentsHoldingsGetResponse> holdingsMono = plaidService.getInvestmentHoldings(accessToken)
                                .filter(Either::isRight)
                                .map(Either::get)
                                .onErrorResume(e -> {
                                    LOG.error("Error fetching investment holdings: {}", e.getMessage());
                                    return Mono.empty();
                                });
                                
                            Mono<InvestmentsTransactionsGetResponse> transactionsMono = plaidService.getInvestmentTransactions(accessToken, 100)
                                .filter(Either::isRight)
                                .map(Either::get)
                                .onErrorResume(e -> {
                                    LOG.error("Error fetching investment transactions: {}", e.getMessage());
                                    return Mono.empty();
                                });
                            
                            // Wait for both to complete and map them together
                            return Mono.zip(holdingsMono, transactionsMono)
                                .map(tuple -> {
                                    InvestmentsHoldingsGetResponse holdings = tuple.getT1();
                                    InvestmentsTransactionsGetResponse transactions = tuple.getT2();
                                    
                                    // Map this specific token's data to assets
                                    return InvestmentHoldingMapper.mapResponseToAccountAssets(holdings, transactions);
                                })
                                .onErrorResume(e -> {
                                    LOG.error("Error processing token {}: {}", accessToken, e.getMessage());
                                    return Mono.just(Collections.emptyList());
                                });
                        })
                        .collect(Collectors.toList());
                    
                    // Combine all assets from all tokens, deduplicating by ID
                    return Flux.fromIterable(assetsMonos)
                        .flatMap(mono -> mono)
                        .reduce(
                            new HashMap<String, AccountAsset>(),
                            (assetMap, assetsList) -> {
                                assetsList.forEach(asset -> {
                                    assetMap.put(asset.getId().id, asset);
                                });
                                return assetMap;
                            }
                        )
                        .map(assetMap -> new ArrayList<>(assetMap.values()))
                        .map(ResponseEntity::ok);
                }
            });
    }


    @GetMapping(path = "/{id}/liabilities", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> getLiabilities(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable("id") String id) {
        LOG.info(userId);
        return integrationService.getUserIntegrations(userId).flatMap(integrationsEither -> {
            if (integrationsEither.isLeft()) {

                CustomError error = integrationsEither.getLeft();
                // If there was an error getting integrations, return empty flux
                return Mono.just(ResponseEntity
                        .status(error.getHttpStatusCode())
                        .body(error.getArgs()));
            } else {
                // For each integration map, extract the access token and call getTransactions
                List<Map<String, Object>> integrations = integrationsEither.get();
                
                List<Mono<LiabilitiesGetResponse>> transactionMonos = integrations.stream()
                        .map(integration -> {
                            String accessToken = (String)integration.get("accessToken");
                            if (accessToken == null) {
                                // Skip this integration if access token is missing
                                return Mono.<LiabilitiesGetResponse>empty();
                            }

                        LOG.info("Access Token: {}", accessToken);

                        return plaidService.getLiabilities(accessToken).filter(Either::isRight)
                            .map(Either::get)
                            //.defaultIfEmpty(null)  // Handle case where Either was Left
                            .onErrorResume(e -> Mono.empty());
                            
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                // Combine all the Monos into a single Mono<List<TransactionsSyncResponse>>
                return Flux.fromIterable(transactionMonos)
                        .flatMap(mono -> mono)
                        .reduceWith(
                            () -> new HashMap<String, AccountAsset>(), 
                            (assetMap, liabilities) -> {
                                // Map transactions to account assets and eliminate duplicates by ID
                                List<AccountAsset> assets = LiabilityMapper.mapResponseToAccountAssets(liabilities);
                                assets.forEach(asset -> {
                                    assetMap.put(asset.getId().id, asset);
                                });
                                return assetMap;
                            }
                        )
                        .map(assetMap -> new ArrayList<>(assetMap.values()))
                        .map(ResponseEntity::ok);
            }
        });

        
        /*return plaidService.getLiabilities(request.accessToken()).flatMap(response -> {
            if (response.isRight()) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.OK) // 201 Created
                        .body(LiabilityMapper.mapResponseToAccountAssets(response.get())));
            } else {
                CustomError customError = response.getLeft();
                LOG.error("Error getHttpStatusCode principal: {}", customError.getHttpStatusCode());
                LOG.error("Error creating link token: {}", customError.getMessage());

                return Mono.just(ResponseEntity
                        .status(customError.getHttpStatusCode()) // Return actual error status
                        .body(customError)); // Return CustomError as response body
            }
        });*/
    }
}
