package com.asbitech.client_ms.interfaces.dto;


public record ActivatePrincipalRequest(String activationToken, String tempPassword, String newPassword) {

}