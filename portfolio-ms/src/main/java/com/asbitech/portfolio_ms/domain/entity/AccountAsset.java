package com.asbitech.portfolio_ms.domain.entity;


import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;



@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class AccountAsset extends Asset {
    String accountNumber;
    String currency;
    String financialInstitution;
    BigDecimal balance;
    List<? extends Transaction> transactions;
    List<InvestmentAsset> holdings;
    List<Liability> liabilities;
}

