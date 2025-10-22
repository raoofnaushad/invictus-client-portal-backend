package com.asbitech.portfolio_ms.infra.external.impl;

import com.asbitech.portfolio_ms.infra.external.IntegrationService;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.asbitech.common.domain.CustomError;
import io.vavr.control.Either;
import reactor.core.publisher.Mono;

@Service
public class IntegrationServiceImpl implements IntegrationService {
    private final WebClient webClient;
    private final String baseUrl = "http://localhost:8083";
    
    public IntegrationServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Mono<Either<CustomError, List<Map<String, Object>>>> getUserIntegrations(String userId) {
        return webClient.get()
                .uri("/api/v1/integrations/plaid")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(Either::right);
    }
    
}
