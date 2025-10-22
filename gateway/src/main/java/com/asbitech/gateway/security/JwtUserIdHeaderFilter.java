package com.asbitech.gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtUserIdHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtUserIdHeaderFilter.class);
    private static final String USER_ID_HEADER = "X-User-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .map(this::extractUserId)
                .defaultIfEmpty("anonymous")
                .flatMap(userId -> {
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, userId)
                            .build();
                    
                    logger.debug("Added user ID header: {}", userId);
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private String extractUserId(Authentication authentication) {
        try {
            JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = (Jwt) jwtAuthToken.getPrincipal();
            
            // Extract subject claim (user ID) from the JWT
            return jwt.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting user ID from authentication", e);
            return "unknown";
        }
    }

    @Override
    public int getOrder() {
        // Set to run after authentication filters but before other business logic
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

