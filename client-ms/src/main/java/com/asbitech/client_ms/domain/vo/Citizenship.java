package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.vo.CodeType;

public record Citizenship(
        CodeType citizenshipCountry,
        IdPaper idPaper,
        Boolean isPrimary
) {

}
