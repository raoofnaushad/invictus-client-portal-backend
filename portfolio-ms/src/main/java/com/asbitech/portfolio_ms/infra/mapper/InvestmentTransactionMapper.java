package com.asbitech.portfolio_ms.infra.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asbitech.portfolio_ms.domain.entity.AccountAsset;
import com.asbitech.portfolio_ms.domain.entity.InvestmentTransaction;
import com.asbitech.portfolio_ms.domain.entity.Transaction;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;
import com.asbitech.portfolio_ms.domain.vo.TransactionId;
import com.plaid.client.model.InvestmentsTransactionsGetResponse;

public class InvestmentTransactionMapper {   
    public static Map<String, List<InvestmentTransaction>> mapResponseToInvestmentTransactions(InvestmentsTransactionsGetResponse response) {
        // Create a map to group transactions by assetId
        Map<String, List<InvestmentTransaction>> transactionsByAssetId = new HashMap<>();
        
        // Process added transactions
        if (response.getInvestmentTransactions() != null) {
            for (com.plaid.client.model.InvestmentTransaction  transaction : response.getInvestmentTransactions()) {
                System.out.println("Added transaction assetId: " +new AssetId(transaction.getAccountId()));

                System.out.println("Added transaction: " + mapPlaidTransaction(transaction, new AssetId(transaction.getAccountId())));
                addToMap(transactionsByAssetId, mapPlaidTransaction(transaction, new AssetId(transaction.getSecurityId())));
            }
        }

        return transactionsByAssetId;        
    }
    

    /**
     * Add a transaction to the map grouped by assetId
     */
    private static void addToMap(Map<String, List<InvestmentTransaction>> map, InvestmentTransaction transaction) {
        String assetId = transaction.getAssetId().id;
        if (assetId != null) {
            map.computeIfAbsent(assetId, k -> new ArrayList<>()).add(transaction);
        }
    }
    

    public static InvestmentTransaction mapPlaidTransaction(com.plaid.client.model.InvestmentTransaction plaidTransaction, AssetId assetId) {
        if (plaidTransaction == null) {
            return null;
        }

        //  tCreateransaction ID from plaid's transaction ID
        TransactionId transactionId = new TransactionId(plaidTransaction.getInvestmentTransactionId());
        
        // Map the date (assuming Plaid provides date in ISO format YYYY-MM-DD)
        LocalDate transactionDate = LocalDate.parse(plaidTransaction.getDate().toString(), DateTimeFormatter.ISO_DATE);
        
        // Map the external ID (using Plaid's transaction ID as external ID)
        String externalId = plaidTransaction.getInvestmentTransactionId();
        
        // Create source data
        SourceData sourceData = SourceData.PLAID;
        
        // Map transaction type
        //TransactionType transactionType = mapTransactionType(plaidTransaction);
        
        // Map amount (Plaid typically returns positive values for debits and negative for credits)
        // We may need to negate the value based on your application's conventions
        BigDecimal amount = BigDecimal.valueOf(plaidTransaction.getAmount());
        
        // Map description
        String description = plaidTransaction.getName();
        
        // Map currency (or use default if not available)
        String currency = plaidTransaction.getIsoCurrencyCode() != null ? 
                plaidTransaction.getIsoCurrencyCode() : "USD";
        
        String type = plaidTransaction.getType().getValue();
        String subType = plaidTransaction.getSubtype().getValue();
        BigDecimal fees =   BigDecimal.valueOf(plaidTransaction.getFees() != null ? plaidTransaction.getFees() : 0);
        Double units = plaidTransaction.getQuantity();

        // Build and return the transaction
        return InvestmentTransaction.builder()
                .id(transactionId)
                .assetId(assetId)
                .date(transactionDate)
                .externalId(externalId)
                .dataSource(sourceData)
                //.type(transactionType)
                .amount(amount)
                .description(description)
                .currency(currency)
                .type(type)
                .subType(subType)
                .fees(fees)
                .units(units)
                .build();
    }
}
