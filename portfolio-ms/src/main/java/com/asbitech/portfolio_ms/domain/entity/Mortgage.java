package com.asbitech.portfolio_ms.domain.entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import com.asbitech.portfolio_ms.domain.vo.LiabilityClass;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Mortgage extends Liability {
    String propertyAddress;
    int termInYears;
    String interestType;
    Double interestPercentage;
    BigDecimal interestAmount;
    BigDecimal paidInterestAmount;
    BigDecimal principalAmount;
    BigDecimal paidPrincipalAmount;

    @Override
    public LiabilityClass getLiabilityClass() {
        return LiabilityClass.MORTAGE;
    }
}

