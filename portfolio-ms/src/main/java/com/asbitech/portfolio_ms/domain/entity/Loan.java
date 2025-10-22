package com.asbitech.portfolio_ms.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import com.asbitech.portfolio_ms.domain.vo.LiabilityClass;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Loan extends Liability {
    private String loanType;
    private BigDecimal interestAmount;
    private BigDecimal paidInterestAmount;
    private BigDecimal principalAmount;
    private BigDecimal paidPrincipalAmount;
    private final double interestPercentage;
    private final String interestType;

    @Override
    public LiabilityClass getLiabilityClass() {
        return LiabilityClass.LOAN;
    }
}
