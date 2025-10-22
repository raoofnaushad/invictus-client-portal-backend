package com.asbitech.client_ms.interfaces.dto;


public record ActivateMfaRequest(String activationToken, String phoneNumber, String channel) {

}