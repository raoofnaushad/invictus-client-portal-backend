package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.vo.CodeType;

public record IdPaper(
    String idPaperNumber,
    String idPaperIssuer,
    String idPaperIssueDate,
    String idPaperExpiryDate,
    CodeType idPaperType
) {}