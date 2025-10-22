package com.asbitech.client_ms.domain.vo;

import java.math.BigDecimal;

public record WealthDetails(          
    BigDecimal estimatedTotalAnnualIncome,
    BigDecimal estimatedAnnualRevenueFromOperations,
    BigDecimal estimatedTotalAssets,
    BigDecimal potentialInvestableAmount
) {

}
