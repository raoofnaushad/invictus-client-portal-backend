package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.vo.CodeType;

public record Email(
    CodeType emailType,
    String emailAddress
) {

}
