package com.asbitech.integration_ms.infra.external.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.asbitech.common.domain.CustomError;
import com.asbitech.integration_ms.infra.external.PlaidService;
import com.plaid.client.ApiClient;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.CreditAccountSubtype;
import com.plaid.client.model.CreditFilter;
import com.plaid.client.model.DepositoryAccountSubtype;
import com.plaid.client.model.DepositoryFilter;
import com.plaid.client.model.InvestmentAccountSubtype;
import com.plaid.client.model.InvestmentFilter;
import com.plaid.client.model.InvestmentsRefreshRequest;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenAccountFilters;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.LoanAccountSubtype;
import com.plaid.client.model.LoanFilter;
import com.plaid.client.model.OtherAccountSubtype;
import com.plaid.client.model.OtherFilter;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;

import io.vavr.control.Either;
import reactor.core.publisher.Mono;
import retrofit2.Response;


@Service
public class PlaidServiceImpl implements PlaidService   {
    private final PlaidApi  plaidClient;
    private final String secret;
    private final String clientId;
    
    public PlaidServiceImpl(
            @Value("${plaid.client.id}") String clientId,
            @Value("${plaid.secret}") String secret,
            @Value("${plaid.environment:sandbox}") String environment) {
                HashMap<String, String> apiKeys = new HashMap<String, String>();
                apiKeys.put("clientId", clientId);
                apiKeys.put("secret", secret);
                this.clientId = clientId;
                this.secret = secret;
                ApiClient apiClient = new ApiClient(apiKeys);
                apiClient.setPlaidAdapter(ApiClient.Sandbox); // or equivalent, depending on which environment you're calling into
                plaidClient = apiClient.createService(PlaidApi.class);
    }
    

    private PlaidApi plaidClient() {
        return plaidClient;
    }
    
    @Override
    public Mono<Either<CustomError, ItemPublicTokenExchangeResponse>> exchangePublicToken(String publicToken) {
        try {
            
            System.out.println("publicToken: " + publicToken);
            System.out.println("clientId: " + clientId);
            System.out.println("secret: " + secret);
            ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest().publicToken(publicToken).clientId(clientId).secret(secret);

            
            Response<ItemPublicTokenExchangeResponse> response = plaidClient().itemPublicTokenExchange(request).execute();

            if (response.isSuccessful()) {
                return Mono.just(Either.right(response.body()));          
            }

            CustomError customError = new CustomError(
                    HttpStatus.BAD_REQUEST,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + response.errorBody() + response.message(), null);
                    
            return Mono.just(Either.left(customError));

        }  catch (Exception e) {

            CustomError customError = new CustomError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTEGRATION_ERROR",
                    "PLAID API error: " + e.getMessage(), null);

            return Mono.just(Either.left(customError));
        
            // TODO: handle exception
        }
          
    }


    @Override
    public Mono<Either<CustomError, LinkTokenCreateResponse>> createLinkToken(String userId, String type) {
        try {

                List<Products> products = new ArrayList<>();
                
                LinkTokenAccountFilters filter = new LinkTokenAccountFilters();
                if (type.equals("Depository")) {
                    products.add(Products.TRANSACTIONS);

                    DepositoryFilter dpFilter = new DepositoryFilter();
                    dpFilter.addAccountSubtypesItem(DepositoryAccountSubtype.SAVINGS);
                    dpFilter.addAccountSubtypesItem(DepositoryAccountSubtype.CHECKING);
                    dpFilter.addAccountSubtypesItem(DepositoryAccountSubtype.EBT);




                    filter.depository(dpFilter);
                }

                if (type.equals("Investment")) {
                    products.add(Products.INVESTMENTS);
                    products.add(Products.TRANSACTIONS);


                    InvestmentFilter inFilter = new InvestmentFilter();
                    inFilter.addAccountSubtypesItem(InvestmentAccountSubtype.ALL);
                    DepositoryFilter dpFilter = new DepositoryFilter();

                    dpFilter.addAccountSubtypesItem(DepositoryAccountSubtype.MONEY_MARKET);

                    filter.investment(inFilter);
                    filter.depository(dpFilter);

                }

                if (type.equals("Credit")) {
                    //products.add(Products.TRANSACTIONS);
                    products.add(Products.LIABILITIES);

                    CreditFilter dpFilter = new CreditFilter();
                    dpFilter.addAccountSubtypesItem(CreditAccountSubtype.ALL);
                    LoanFilter lFilter = new LoanFilter();
                    lFilter.addAccountSubtypesItem(LoanAccountSubtype.ALL);
                    filter.credit(dpFilter);
                    filter.loan(lFilter);
                }

                if (type.equals("Cryptocurrency")) {
                    products.add(Products.INVESTMENTS);

                    InvestmentFilter dpFilter = new InvestmentFilter();
                    dpFilter.addAccountSubtypesItem(InvestmentAccountSubtype.ALL);

                    filter.investment(dpFilter);

                }

                if (type.equals("Houses & Car")) {
                    products.add(Products.TRANSACTIONS);

                    OtherFilter dpFilter = new OtherFilter();
                    dpFilter.addAccountSubtypesItem(OtherAccountSubtype.ALL);

                    filter.other(dpFilter);

                }

                if (type.equals("Other")) {
                    products.add(Products.TRANSACTIONS);

                    OtherFilter dpFilter = new OtherFilter();
                    dpFilter.addAccountSubtypesItem(OtherAccountSubtype.ALL);
                    filter.other(dpFilter);
                }

                System.out.println(type);
                System.out.println(products);
                System.out.println(filter);

            

            LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(new LinkTokenCreateRequestUser().clientUserId(userId))
                .clientName(userId)
                .products(products)
                .accountFilters(filter)
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en")
                .webhook("https://example.com/webhook")
                .linkCustomizationName("default");            

            Response<LinkTokenCreateResponse> response = plaidClient().linkTokenCreate(request).execute();

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
