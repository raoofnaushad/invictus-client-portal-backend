package com.asbitech.portfolio_ms.infra.external;

import java.util.List;
import java.util.Map;

import com.asbitech.common.domain.CustomError;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface IntegrationService {
     Mono<Either<CustomError, List<Map<String, Object>>>> getUserIntegrations(String userId);
}
