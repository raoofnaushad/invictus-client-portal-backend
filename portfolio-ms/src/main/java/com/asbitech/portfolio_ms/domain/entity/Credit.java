package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;

import com.asbitech.portfolio_ms.domain.vo.LiabilityClass;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Credit extends Liability {
    private BigDecimal interestAmount;
    private BigDecimal minPaymentAmount;
    private BigDecimal lastStatementBalance;
    private final double interestRate;
    private final String interestType;

    @Override
    public LiabilityClass getLiabilityClass() {
        return LiabilityClass.CREDIT_LINE;
    }
}
