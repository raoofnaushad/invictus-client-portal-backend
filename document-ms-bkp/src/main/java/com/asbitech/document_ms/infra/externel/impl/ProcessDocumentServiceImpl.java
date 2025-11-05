package com.asbitech.document_ms.infra.externel.impl;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.asbitech.document_ms.infra.externel.ProcessDocumentService;
import com.asbitech.document_ms.infra.externel.dto.ExtractRequest;

import reactor.core.publisher.Mono;


@Service
public class ProcessDocumentServiceImpl implements ProcessDocumentService {
    private final WebClient webClient;

    public ProcessDocumentServiceImpl(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8000").build();
    }

    @Override
    public Mono<Map> processDocument(String filePath) {
        ExtractRequest request = new ExtractRequest(filePath);

        return webClient.post()
                .uri("/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class); 
    }

}
