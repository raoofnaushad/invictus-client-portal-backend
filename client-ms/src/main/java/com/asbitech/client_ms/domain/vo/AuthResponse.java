package com.asbitech.client_ms.domain.vo;

public record AuthResponse(
    String accessToken,
    int expiresIn,
    int refreshExpiresIn,
    String refreshToken,
    String tokenType,
    int notBeforePolicy,
    String sessionState,
    String scope
) {
    // You can add any additional methods if needed
}
