package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.asbitech.portfolio_ms.domain.vo.AssetId;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;



@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class InvestmentAsset extends Asset {
    
    private final String securityId;
    private final String ticker;
    private final String custodian;
    private final BigDecimal aquisitionValue;
    private final AssetId accountId; // used to group investments by account. example in plaid grouped on investment account
    private final String financialInstitution;
    private BigDecimal currentValue;
    private LocalDate currentValueDate;
    private String currency;
    private Double units;
    List<InvestmentTransaction> transactions;

}
