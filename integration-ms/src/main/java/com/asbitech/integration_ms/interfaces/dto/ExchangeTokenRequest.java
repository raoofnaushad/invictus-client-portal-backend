package com.asbitech.integration_ms.interfaces.dto;

import java.util.List;

public record ExchangeTokenRequest(String publicToken, List<String> products, String institutionName) {
    
}
