package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.asbitech.common.domain.Entity;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.LiabilityClass;
import com.asbitech.portfolio_ms.domain.vo.LiabilityId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Liability extends Entity<LiabilityId> {
    private final String name;
    private final LiabilityClass liabilityClass;
    private final String externalId;
    private final String financialInstitution;
    private SourceData dataSource;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final LocalDate originalDate;
    private final AssetId accountId;
    private final LocalDate lastPaymentDate;
    private final LocalDate nextPaymentDate;
    private final BigDecimal lastPaymentAmount;
}
