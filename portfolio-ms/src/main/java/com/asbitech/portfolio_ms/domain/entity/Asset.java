package com.asbitech.portfolio_ms.domain.entity;

import java.time.LocalDate;

import com.asbitech.common.domain.Entity;
import com.asbitech.portfolio_ms.domain.vo.AssetId;
import com.asbitech.portfolio_ms.domain.vo.SourceData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
public class Asset extends Entity<AssetId> {
    private String name;
    private String assetClass;
    private String externalId;
    private SourceData dataSource;
    private String assetSubclass;
    private LocalDate acquisitionDate;
}
