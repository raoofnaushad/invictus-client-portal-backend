package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.asbitech.common.domain.Entity;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;
import com.asbitech.portfolio_ms.domain.vo.TransactionId;
import com.asbitech.portfolio_ms.domain.vo.TransactionType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Transaction extends Entity<TransactionId> {
    private final AssetId assetId;
    private final LocalDate date;
    private final String externalId;
    private SourceData dataSource;
    private final String type;
    private final String category;
    private final String senderRecipient;
    private final String subType;
    private final BigDecimal amount;
    private final String description;
    private final String currency;
}