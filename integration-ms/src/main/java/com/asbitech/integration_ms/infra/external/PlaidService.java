package com.asbitech.integration_ms.infra.external;

import java.util.List;

import com.asbitech.common.domain.CustomError;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface PlaidService {
    Mono<Either<CustomError, ItemPublicTokenExchangeResponse>> exchangePublicToken(String publicToken);
    Mono<Either<CustomError, LinkTokenCreateResponse>> createLinkToken(String userId, String type);
}
