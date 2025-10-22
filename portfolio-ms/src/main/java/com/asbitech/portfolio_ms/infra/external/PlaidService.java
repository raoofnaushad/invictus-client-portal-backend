package com.asbitech.portfolio_ms.infra.external;

import com.asbitech.common.domain.CustomError;
import com.plaid.client.model.InvestmentsHoldingsGetResponse;
import com.plaid.client.model.InvestmentsTransactionsGetResponse;
import com.plaid.client.model.LiabilitiesGetResponse;
import com.plaid.client.model.TransactionsSyncResponse;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;

public interface PlaidService {
        Mono<Either<CustomError, TransactionsSyncResponse>> getTransactions(String accessToken, int count);
        Mono<Either<CustomError, InvestmentsHoldingsGetResponse>> getInvestmentHoldings(String accessToken);
        Mono<Either<CustomError, LiabilitiesGetResponse>> getLiabilities(String accessToken);
        Mono<Either<CustomError, InvestmentsTransactionsGetResponse>> getInvestmentTransactions(String accessToken, int count);
}
