package com.asbitech.portfolio_ms.infra.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asbitech.portfolio_ms.domain.entity.AccountAsset;
import com.asbitech.portfolio_ms.domain.entity.Transaction;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;
import com.asbitech.portfolio_ms.domain.vo.TransactionId;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.TransactionsSyncResponse;
// Removed alias import to avoid collision

public class TransactionSyncMapper {

    /**
     * Maps a TransactionsSyncResponse to a list of AccountAsset objects
     * 
     * @param response The TransactionsSyncResponse from the API
     * @return A list of updated AccountAsset objects
     */
    public static List<AccountAsset> mapResponseToAccountAssets(TransactionsSyncResponse response) {
        // Create a map to group transactions by assetId
        Map<String, List<Transaction>> transactionsByAssetId = new HashMap<>();
        
        // Process added transactions
        if (response.getAdded() != null) {
            for (com.plaid.client.model.Transaction transaction : response.getAdded()) {
                System.out.println("Added transaction assetId: " +new AssetId(transaction.getAccountId()));

                System.out.println("Added transaction: " + mapPlaidTransaction(transaction, new AssetId(transaction.getAccountId())));
                addToMap(transactionsByAssetId, mapPlaidTransaction(transaction, new AssetId(transaction.getAccountId())));
            }
        }
        
        // Process modified transactions
        if (response.getModified() != null) {
            for (com.plaid.client.model.Transaction transaction : response.getModified()) {
                addToMap(transactionsByAssetId, mapPlaidTransaction(transaction, new AssetId(transaction.getAccountId())));
            }
        }
        
        // Map AccountBase objects to AccountAsset objects
        List<AccountAsset> accountAssets = new ArrayList<>();
        if (response.getAccounts() != null) {
            for (AccountBase accountBase : response.getAccounts()) {
                AccountAsset accountAsset = mapAccountBaseToAccountAsset(accountBase);
                
                // Add transactions for this account if available
                AssetId assetId = new AssetId(accountBase.getAccountId());                
                if (transactionsByAssetId.containsKey(assetId.id)) {
                    accountAsset.setTransactions(transactionsByAssetId.get(assetId.id));
                }
                
                accountAssets.add(accountAsset);
            }
        }
        
        return accountAssets;
    }
    
    
    /**
     * Maps an AccountBase object to an AccountAsset
     */
    private  static AccountAsset mapAccountBaseToAccountAsset(AccountBase accountBase) {

        return AccountAsset.builder()
                .id(new AssetId(accountBase.getAccountId()))
                .accountNumber(accountBase.getMask())
                .name(accountBase.getName())
                .externalId(accountBase.getAccountId())
                .financialInstitution("")
                .assetClass(accountBase.getType().toString())
                .assetSubclass(accountBase.getSubtype() != null ? accountBase.getSubtype().toString() : "Unknown")
                .balance(accountBase.getBalances().getAvailable() != null ? BigDecimal.valueOf(accountBase.getBalances().getAvailable()) : BigDecimal.ZERO)
                .currency(accountBase.getBalances().getIsoCurrencyCode() != null ? accountBase.getBalances().getIsoCurrencyCode() : "USD")
                .build();
            }
    
    /**
     * Add a transaction to the map grouped by assetId
     */
    private static void addToMap(Map<String, List<Transaction>> map, Transaction transaction) {
        String assetId = transaction.getAssetId().id;
        if (assetId != null) {
            map.computeIfAbsent(assetId, k -> new ArrayList<>()).add(transaction);
        }
    }
    

    public static Transaction mapPlaidTransaction(com.plaid.client.model.Transaction plaidTransaction, AssetId assetId) {
        if (plaidTransaction == null) {
            return null;
        }

        // Create transaction ID from plaid's transaction ID
        TransactionId transactionId = new TransactionId(plaidTransaction.getTransactionId());
        
        // Map the date (assuming Plaid provides date in ISO format YYYY-MM-DD)
        LocalDate transactionDate = LocalDate.parse(plaidTransaction.getDate().toString(), DateTimeFormatter.ISO_DATE);
        
        // Map the external ID (using Plaid's transaction ID as external ID)
        String externalId = plaidTransaction.getTransactionId();
        
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
        String senderRec = null;
        if (plaidTransaction.getCounterparties() != null && !plaidTransaction.getCounterparties().isEmpty()) {
            senderRec = plaidTransaction.getCounterparties().get(0).getName();
        }
        
        // Build and return the transaction
        return Transaction.builder()
                .id(transactionId)
                .assetId(assetId)
                .date(transactionDate)
                .externalId(externalId)
                .dataSource(sourceData)
                .senderRecipient(senderRec)
                .category(plaidTransaction.getPersonalFinanceCategory() != null ? plaidTransaction.getPersonalFinanceCategory().getPrimary() : "")
                .type(plaidTransaction.getTransactionType() != null ? plaidTransaction.getTransactionType().toString() : "unknown")
                .amount(amount)
                .description(description)
                .currency(currency)
                .build();
    }
}
