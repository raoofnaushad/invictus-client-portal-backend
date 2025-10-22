package com.asbitech.document_ms.infra.externel;

import java.util.Map;

import reactor.core.publisher.Mono;

public interface ProcessDocumentService {
    Mono<Map> processDocument(String filePath);
}
