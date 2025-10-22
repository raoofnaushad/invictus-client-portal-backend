package com.asbitech.portfolio_ms.infra.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asbitech.portfolio_ms.domain.entity.AccountAsset;
import com.asbitech.portfolio_ms.domain.entity.InvestmentAsset;
import com.asbitech.portfolio_ms.domain.entity.InvestmentTransaction;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.InvestmentsHoldingsGetResponse;
import com.plaid.client.model.InvestmentsTransactionsGetResponse;
import com.plaid.client.model.Security;

public class InvestmentHoldingMapper {
    /**
     * Maps a TransactionsSyncResponse to a list of AccountAsset objects
     * 
     * @param response The TransactionsSyncResponse from the API
     * @return A list of updated AccountAsset objects
     */
    public static List<AccountAsset> mapResponseToAccountAssets(InvestmentsHoldingsGetResponse response, InvestmentsTransactionsGetResponse transactionsResponse) {
        // Create a map to group transactions by assetId
        Map<String, List<InvestmentAsset>> investmentByAssetId = new HashMap<>();
        System.out.println("Added holding assetId A: " +response);
        System.out.println("Added holding assetId: B " + transactionsResponse);
        // Process added holding
        if (response.getHoldings() != null) {
            for (com.plaid.client.model.Holding holding : response.getHoldings()) {
                System.out.println("Added holding assetId: " +new AssetId(holding.getAccountId()));

                addToMap(investmentByAssetId, mapPlaidHolding(holding, response.getSecurities() ,new AssetId(holding.getAccountId())));
            }
        }

        Map<String, List<InvestmentTransaction>> invTransaction = InvestmentTransactionMapper.mapResponseToInvestmentTransactions(transactionsResponse);

        // Map AccountBase objects to AccountAsset objects
        List<AccountAsset> accountAssets = new ArrayList<>();
        if (response.getAccounts() != null) {
            for (AccountBase accountBase : response.getAccounts()) {
                AccountAsset accountAsset = mapAccountBaseToAccountAsset(accountBase);
                accountAsset.setFinancialInstitution(response.getItem().getInstitutionName());
                // Add transactions for this account if available
                AssetId assetId = new AssetId(accountBase.getAccountId());
               
                
                if (investmentByAssetId.containsKey(assetId.id)) {

                    List<InvestmentAsset> investmentAssets = investmentByAssetId.get(assetId.id);

                    investmentAssets.forEach(accountAssetI -> {
                        if (invTransaction.containsKey(accountAssetI.getSecurityId())) { 
                            
                            accountAssetI.setTransactions(invTransaction.get(accountAssetI.getSecurityId()));
                        }

                    });


                    accountAsset.setHoldings(investmentAssets);
                    accountAssets.add(accountAsset);
                }
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
    private static void addToMap(Map<String, List<InvestmentAsset>> map, InvestmentAsset investmentAsset) {
        String assetId = investmentAsset.getAccountId().id;
        if (assetId != null) {
            map.computeIfAbsent(assetId, k -> new ArrayList<>()).add(investmentAsset);
        }
    }
    

    public static InvestmentAsset mapPlaidHolding(com.plaid.client.model.Holding plaidHolding, List<Security> securities, AssetId assetId) {
        if (plaidHolding == null) {
            return null;
        }

        AssetId investmentAssetId = new AssetId(plaidHolding.getAccountId() + "-" + plaidHolding.getSecurityId());
        
        Security plaidSecurity = getSecurityById(securities, plaidHolding.getSecurityId());
    
        LocalDate priceDate = plaidHolding.getInstitutionPriceDatetime() != null
                ? LocalDate.parse(plaidHolding.getInstitutionPriceDatetime().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        // Build and return the transaction
        return InvestmentAsset.builder()
                .id(investmentAssetId)
                .ticker(plaidSecurity != null ? plaidSecurity.getTickerSymbol() : null)
                .name(plaidSecurity != null ? plaidSecurity.getName() : null)
                .assetClass("investment")
                .assetSubclass(plaidSecurity != null ? plaidSecurity.getType() : "other")          
                .accountId(assetId)
                .dataSource(SourceData.PLAID)
                .externalId(investmentAssetId.id)
                .securityId(plaidHolding.getSecurityId())
                .acquisitionDate(null)
                .aquisitionValue(plaidHolding.getCostBasis() != null ? BigDecimal.valueOf(plaidHolding.getCostBasis()) : null)  
                .currentValue(plaidHolding.getInstitutionPrice() != null ? BigDecimal.valueOf(plaidHolding.getInstitutionPrice()) : null)
                .units(plaidHolding.getQuantity() != null ? plaidHolding.getQuantity() : null)
                .currentValueDate(priceDate)
                .currency(plaidHolding.getIsoCurrencyCode())
                .build();
    }


    private static Security getSecurityById(List<Security> securities, String securityId) {
        if (securities == null) {
            return null;
        }

        for (Security security : securities) {
            if (security.getSecurityId().equals(securityId)) {
                return security;
            }
        }
        return null;
    }

}
