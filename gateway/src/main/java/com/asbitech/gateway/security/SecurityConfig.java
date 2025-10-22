package com.asbitech.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                // Public endpoints (ignored from auth)
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/v1/principals/public/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()  // Actuator health/monitoring endpoints

                // Everything else requires authentication
                .anyExchange().authenticated()
            ).cors()  // Enable CORS (make sure Spring's CORS support is enabled)
            .and()
            // Enable JWT Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }
}
