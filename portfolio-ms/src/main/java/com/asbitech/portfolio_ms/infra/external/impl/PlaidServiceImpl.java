package com.asbitech.portfolio_ms.infra.external.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.asbitech.common.domain.CustomError;
import com.asbitech.portfolio_ms.infra.external.PlaidService;
import com.plaid.client.ApiClient;
import com.plaid.client.model.InvestmentsHoldingsGetRequest;
import com.plaid.client.model.InvestmentsHoldingsGetResponse;
import com.plaid.client.model.InvestmentsTransactionsGetRequest;
import com.plaid.client.model.InvestmentsTransactionsGetRequestOptions;
import com.plaid.client.model.InvestmentsTransactionsGetResponse;
import com.plaid.client.model.LiabilitiesGetRequest;
import com.plaid.client.model.LiabilitiesGetResponse;
import com.plaid.client.model.TransactionsSyncRequest;
import com.plaid.client.model.TransactionsSyncResponse;
import com.plaid.client.request.PlaidApi;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;
import retrofit2.Response;


@Service
public class PlaidServiceImpl implements PlaidService {

    private final PlaidApi  plaidClient;
    
    public PlaidServiceImpl(
            @Value("${plaid.client.id}") String clientId,
            @Value("${plaid.secret}") String secret,
            @Value("${plaid.environment:sandbox}") String environment) {
                HashMap<String, String> apiKeys = new HashMap<String, String>();
                apiKeys.put("clientId", clientId);
                apiKeys.put("secret", secret);
                ApiClient apiClient = new ApiClient(apiKeys);
                apiClient.setPlaidAdapter(ApiClient.Sandbox); // or equivalent, depending on which environment you're calling into
                plaidClient = apiClient.createService(PlaidApi.class);
    }
    

    private PlaidApi plaidClient() {
        return plaidClient;
    }
    
    @Override
    @Cacheable(value = "getTransactions", key = "#accessToken")
    public Mono<Either<CustomError, TransactionsSyncResponse>> getTransactions(String accessToken, int count) {
        try {

            TransactionsSyncRequest request = new TransactionsSyncRequest()
                    .accessToken(accessToken)
                    .count(count);


            Response<TransactionsSyncResponse> response = plaidClient().transactionsSync(request).execute();

            if (response.isSuccessful()) {
                return Mono.just(Either.right(response.body()));          
            }

            CustomError customError = new CustomError(
                    HttpStatus.BAD_REQUEST,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + response.errorBody(), null);
                    
            return Mono.just(Either.left(customError));

            
        }    catch (Exception e) {

            CustomError customError = new CustomError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + e.getMessage(), null);

            return Mono.just(Either.left(customError));
        
            // TODO: handle exception
        }
    }

    @Cacheable(value = "getInvestmentHoldings", key = "#accessToken")
    public Mono<Either<CustomError, InvestmentsHoldingsGetResponse>> getInvestmentHoldings(String accessToken) {
        try {

            InvestmentsHoldingsGetRequest request = new InvestmentsHoldingsGetRequest()
                    .accessToken(accessToken)/*.options(new InvestmentHoldingsGetRequestOptions()
                            .accountIds(count)) */;


            Response<InvestmentsHoldingsGetResponse> response = plaidClient().investmentsHoldingsGet(request).execute();

            System.out.println("response" + response);

            if (response.isSuccessful()) {
                return Mono.just(Either.right(response.body()));          
            }

            CustomError customError = new CustomError(
                    HttpStatus.BAD_REQUEST,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + response.errorBody(), null);
                    
            return Mono.just(Either.left(customError));

            
        }    catch (Exception e) {

            CustomError customError = new CustomError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + e.getMessage(), null);

            return Mono.just(Either.left(customError));
        }
    }
    


    @Cacheable(value = "getInvestmentTransactions", key = "#accessToken")
    public Mono<Either<CustomError, InvestmentsTransactionsGetResponse>> getInvestmentTransactions(String accessToken, int count) {
        try {

             LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(3);


            InvestmentsTransactionsGetRequest request = new InvestmentsTransactionsGetRequest()
                    .accessToken(accessToken).startDate(startDate).endDate(endDate).options(new InvestmentsTransactionsGetRequestOptions()
                            .count(count));


            Response<InvestmentsTransactionsGetResponse> response = plaidClient().investmentsTransactionsGet(request).execute();
            System.out.println("response 2" + response);

            if (response.isSuccessful()) {
                return Mono.just(Either.right(response.body()));          
            }

            CustomError customError = new CustomError(
                    HttpStatus.BAD_REQUEST,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + response.errorBody(), null);
                    
            return Mono.just(Either.left(customError));

            
        }    catch (Exception e) {

            CustomError customError = new CustomError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + e.getMessage(), null);

            return Mono.just(Either.left(customError));
        }
    }

    @Override
    @Cacheable(value = "getLiabilities", key = "#accessToken")
    public Mono<Either<CustomError, LiabilitiesGetResponse>> getLiabilities(String accessToken) {
        try {

            LiabilitiesGetRequest request = new LiabilitiesGetRequest()
                    .accessToken(accessToken)/*.options(new InvestmentHoldingsGetRequestOptions()
                            .accountIds(count)) */;


            Response<LiabilitiesGetResponse> response = plaidClient().liabilitiesGet(request).execute();

            if (response.isSuccessful()) {
                return Mono.just(Either.right(response.body()));          
            }

            CustomError customError = new CustomError(
                    HttpStatus.BAD_REQUEST,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + response.errorBody(), null);
                    
            return Mono.just(Either.left(customError));

            
        }    catch (Exception e) {

            CustomError customError = new CustomError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + e.getMessage(), null);

            return Mono.just(Either.left(customError));
        }
    }
    
    
}
