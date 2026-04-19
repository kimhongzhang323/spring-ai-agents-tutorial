package com.masterclass.mcp.config;

import com.masterclass.shared.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the MCP server.
 *
 * Transport endpoints:
 *   GET  /sse     — SSE connection establishment (public: client connects here first)
 *   POST /message — JSON-RPC message delivery   (protected: requires JWT bearer token)
 *
 * Rationale: the SSE endpoint itself carries no user data; the client establishes
 * the channel and then sends all requests via POST /message.
 * Protecting /message with JWT prevents unauthorised tool invocations.
 */
@Configuration
@EnableWebSecurity
public class McpSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public McpSecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: SSE connection, actuator health, OpenAPI
                        .requestMatchers("/sse", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // All other endpoints (including /message) require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
