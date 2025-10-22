package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.vo.CodeType;


public record BusinessSpecInfo(
        String name,
        CodeType legalForm,
        String registrationNumber,
        String taxNumber,
        String incorporationDate,
        CodeType countryOfIncorporation
            // TODO add more fields
) {
}