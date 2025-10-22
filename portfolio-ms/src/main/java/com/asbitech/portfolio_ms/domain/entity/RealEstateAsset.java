package com.asbitech.portfolio_ms.domain.entity;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;



@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class RealEstateAsset extends Asset {
    private final String address;
    private final String propertyType;
    private final BigDecimal aquisitionValue;
    private final BigDecimal currentValue;
    private final double squareFootage;
}
