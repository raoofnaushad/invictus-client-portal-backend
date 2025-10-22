package com.asbitech.portfolio_ms.infra.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cglib.core.Local;

import com.asbitech.portfolio_ms.domain.entity.AccountAsset;
import com.asbitech.portfolio_ms.domain.entity.Credit;
import com.asbitech.portfolio_ms.domain.entity.InvestmentAsset;
import com.asbitech.portfolio_ms.domain.entity.Liability;
import com.asbitech.portfolio_ms.domain.entity.Loan;
import com.asbitech.portfolio_ms.domain.entity.Mortgage;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.LiabilityId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.CreditCardLiability;
import com.plaid.client.model.LiabilitiesGetResponse;
import com.plaid.client.model.MortgageLiability;
import com.plaid.client.model.StudentLoan;

public class LiabilityMapper {
    /**
     * Maps a TransactionsSyncResponse to a list of AccountAsset objects
     * 
     * @param response The TransactionsSyncResponse from the API
     * @return A list of updated AccountAsset objects
     */
    public static List<AccountAsset> mapResponseToAccountAssets(LiabilitiesGetResponse response) {
        // Create a map to group transactions by assetId
        Map<String, List<Liability>> liabilityByAssetId = new HashMap<>();
        
        // Process added holding
        if (response.getLiabilities() != null) {
            if (response.getLiabilities().getCredit() != null) {
                for (CreditCardLiability credit :response.getLiabilities().getCredit()) {
                    System.out.println("Added holding assetId: " +new AssetId(credit.getAccountId()));
    
                    addToMap(liabilityByAssetId, mapCreditCardLiability(credit, new AssetId(credit.getAccountId())));
                }

            }

            if (response.getLiabilities().getMortgage() != null) {
                for (MortgageLiability credit :response.getLiabilities().getMortgage()) {
                    System.out.println("Added liability assetId: " +new AssetId(credit.getAccountId()));
    
                    addToMap(liabilityByAssetId, mapMortageLiability(credit, new AssetId(credit.getAccountId())));
                }
            }


            if (response.getLiabilities().getStudent() != null) {
                for (StudentLoan credit :response.getLiabilities().getStudent()) {
                    System.out.println("Added liability assetId: " +new AssetId(credit.getAccountId()));
    
                    addToMap(liabilityByAssetId, mapLoanLiability(credit, new AssetId(credit.getAccountId())));
                }
            }


            
        }



        // Map AccountBase objects to AccountAsset objects
        List<AccountAsset> accountAssets = new ArrayList<>();
        if (response.getAccounts() != null) {
            for (AccountBase accountBase : response.getAccounts()) {
                AccountAsset accountAsset = mapAccountBaseToAccountAsset(accountBase);
                
                // Add transactions for this account if available
                AssetId assetId = new AssetId(accountBase.getAccountId());
               
                
                if (liabilityByAssetId.containsKey(assetId.id)) {
                        accountAsset.setFinancialInstitution(response.getItem().getInstitutionName());
                        accountAsset.setLiabilities(liabilityByAssetId.get(assetId.id));
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
    private static void addToMap(Map<String, List<Liability>> map, Liability liability) {
        String assetId = liability.getAccountId().id;
        if (assetId != null) {
            map.computeIfAbsent(assetId, k -> new ArrayList<>()).add(liability);
        }
    }

    public static Credit mapCreditCardLiability(CreditCardLiability plaidCreditCardLiability,  AssetId assetId) {
        if (plaidCreditCardLiability == null) {
            return null;
        }

        LiabilityId liabilityId = new LiabilityId(plaidCreditCardLiability.getAccountId());
        
        LocalDate lastPaymentDate = plaidCreditCardLiability.getLastPaymentDate() != null
                ? LocalDate.parse(plaidCreditCardLiability.getLastPaymentDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;
        BigDecimal lastPaymentAmount = plaidCreditCardLiability.getLastPaymentAmount() != null
                ? BigDecimal.valueOf(plaidCreditCardLiability.getLastPaymentAmount())
                : null;

        LocalDate nextPaymentDate = plaidCreditCardLiability.getNextPaymentDueDate() != null
                ? LocalDate.parse(plaidCreditCardLiability.getNextPaymentDueDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        BigDecimal lastStatementAmount = plaidCreditCardLiability.getLastStatementBalance() != null
                ? BigDecimal.valueOf(plaidCreditCardLiability.getLastStatementBalance())
                : null;

        BigDecimal minPaymentAmount = plaidCreditCardLiability.getMinimumPaymentAmount() != null
                ? BigDecimal.valueOf(plaidCreditCardLiability.getMinimumPaymentAmount())
                : null;
       
        return Credit.builder()
                .id(liabilityId)
                .externalId(liabilityId.id)
                .accountId(assetId)
                
                .dataSource(SourceData.PLAID)
                .lastPaymentDate(lastPaymentDate)
                .lastPaymentAmount(lastPaymentAmount)
                .lastStatementBalance(lastStatementAmount)
                .nextPaymentDate(nextPaymentDate)
                .minPaymentAmount(minPaymentAmount)
                .build();
    }
    

    public static Mortgage mapMortageLiability(MortgageLiability mortageLiability,  AssetId assetId) {
        if (mortageLiability == null) {
            return null;
        }

        LiabilityId liabilityId = new LiabilityId(mortageLiability.getAccountId());
        
        LocalDate lastPaymentDate = mortageLiability.getLastPaymentDate() != null
                ? LocalDate.parse(mortageLiability.getLastPaymentDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;
        BigDecimal lastPaymentAmount = mortageLiability.getLastPaymentAmount() != null
                ? BigDecimal.valueOf(mortageLiability.getLastPaymentAmount())
                : null;

        LocalDate nextPaymentDate = mortageLiability.getNextPaymentDueDate() != null
                ? LocalDate.parse(mortageLiability.getNextPaymentDueDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        LocalDate originalDate = mortageLiability.getOriginationDate() != null
                ? LocalDate.parse(mortageLiability.getOriginationDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        LocalDate maturityDate = mortageLiability.getMaturityDate() != null
                ? LocalDate.parse(mortageLiability.getMaturityDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        String interestType = mortageLiability.getInterestRate() != null
                ? mortageLiability.getInterestRate().getType()
                : null;

        Double interestPercentage = mortageLiability.getInterestRate() != null
                ? mortageLiability.getInterestRate().getPercentage()
                : null;
       
        BigDecimal principalAmount = mortageLiability.getOriginationPrincipalAmount() != null
                ? BigDecimal.valueOf(mortageLiability.getOriginationPrincipalAmount())
                : null;

        BigDecimal paidPrincipal = mortageLiability.getYtdPrincipalPaid() != null
                ? BigDecimal.valueOf(mortageLiability.getYtdPrincipalPaid())
                : null;

        BigDecimal paidInterest = mortageLiability.getYtdInterestPaid() != null
                ? BigDecimal.valueOf(mortageLiability.getYtdInterestPaid())
                : null;

        String propertyAddress = null;
        if (mortageLiability.getPropertyAddress() == null) {
            propertyAddress = String.format("%s, %s, %s %s, %s", mortageLiability.getPropertyAddress().getStreet(), mortageLiability.getPropertyAddress().getCity(), mortageLiability.getPropertyAddress().getRegion(), mortageLiability.getPropertyAddress().getPostalCode(), mortageLiability.getPropertyAddress().getCountry());
        }

       
        return Mortgage.builder()
                .id(liabilityId)
                .externalId(liabilityId.id)
                .accountId(assetId)
                .dataSource(SourceData.PLAID)
                .interestPercentage(interestPercentage)
                .interestType(interestType)
                .lastPaymentDate(lastPaymentDate)
                .lastPaymentAmount(lastPaymentAmount)
                .nextPaymentDate(nextPaymentDate)
                .originalDate(originalDate)
                .principalAmount(principalAmount)
                .maturityDate(maturityDate)
                .paidPrincipalAmount(paidPrincipal)
                .paidInterestAmount(paidInterest)
                .propertyAddress(propertyAddress)
                .build();
    }

    public static Loan mapLoanLiability(StudentLoan StudentLiability,  AssetId assetId) {
        if (StudentLiability == null) {
            return null;
        }

        LiabilityId liabilityId = new LiabilityId(StudentLiability.getAccountId());
        
        LocalDate lastPaymentDate = StudentLiability.getLastPaymentDate() != null
                ? LocalDate.parse(StudentLiability.getLastPaymentDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;
        BigDecimal lastPaymentAmount = StudentLiability.getLastPaymentAmount() != null
                ? BigDecimal.valueOf(StudentLiability.getLastPaymentAmount())
                : null;

        LocalDate nextPaymentDate = StudentLiability.getNextPaymentDueDate() != null
                ? LocalDate.parse(StudentLiability.getNextPaymentDueDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        LocalDate originalDate = StudentLiability.getOriginationDate() != null
                ? LocalDate.parse(StudentLiability.getOriginationDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        LocalDate maturityDate = StudentLiability.getLoanStatus() != null && StudentLiability.getLoanStatus().getEndDate() != null
                ? LocalDate.parse(StudentLiability.getLoanStatus().getEndDate().toString(), DateTimeFormatter.ISO_DATE)
                : null;

        Double interestPercentage = StudentLiability.getInterestRatePercentage() != null
                ? StudentLiability.getInterestRatePercentage()
                : null;
       
        BigDecimal principalAmount = StudentLiability.getOriginationPrincipalAmount() != null
                ? BigDecimal.valueOf(StudentLiability.getOriginationPrincipalAmount())
                : null;
        
        BigDecimal interestAmount = StudentLiability.getOutstandingInterestAmount() != null
                ? BigDecimal.valueOf(StudentLiability.getOutstandingInterestAmount())
                : null;

        BigDecimal paidPrincipal = StudentLiability.getYtdPrincipalPaid() != null
                ? BigDecimal.valueOf(StudentLiability.getYtdPrincipalPaid())
                : null;

        BigDecimal paidInterest = StudentLiability.getYtdInterestPaid() != null
                ? BigDecimal.valueOf(StudentLiability.getYtdInterestPaid())
                : null;

       
        return Loan.builder()
                .id(liabilityId)
                .externalId(liabilityId.id)
                .accountId(assetId)
                .dataSource(SourceData.PLAID)
                .interestPercentage(interestPercentage)
                .lastPaymentDate(lastPaymentDate)
                .lastPaymentAmount(lastPaymentAmount)
                .nextPaymentDate(nextPaymentDate)
                .originalDate(originalDate)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .loanType("Student Loan")
                .maturityDate(maturityDate)
                .paidPrincipalAmount(paidPrincipal)
                .paidInterestAmount(paidInterest)
                .build();
    }

}
