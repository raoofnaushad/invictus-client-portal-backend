package com.asbitech.client_ms.commands;

public record CreateUserCommand(String username, String password, Boolean isPrincipal) {
}
