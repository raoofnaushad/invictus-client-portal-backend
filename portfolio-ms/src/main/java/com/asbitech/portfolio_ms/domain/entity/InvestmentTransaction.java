package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class InvestmentTransaction extends Transaction {
    private final BigDecimal pricePerUnit;
    private final BigDecimal fees;
    private final Double units;
}
