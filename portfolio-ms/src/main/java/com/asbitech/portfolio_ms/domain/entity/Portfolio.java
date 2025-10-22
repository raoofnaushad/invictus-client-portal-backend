package com.asbitech.portfolio_ms.domain.entity;

import java.util.List;

import com.asbitech.common.domain.Entity;
import com.asbitech.portfolio_ms.domain.vo.PortfolioId;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;



@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Portfolio extends Entity<PortfolioId> {
    private String name;
    private String principalId;
    private String managerId;
    private List<Asset> assets;
    private List<Liability> liabilities;
    private String description;    
}
