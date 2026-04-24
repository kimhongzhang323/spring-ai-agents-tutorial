package com.masterclass.capstone.config;

import com.masterclass.shared.security.JwtAuthFilter;
import com.masterclass.shared.ratelimit.RateLimitFilter;
import com.masterclass.shared.ratelimit.RateLimitProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class CapstoneSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitProperties rateLimitProperties;
    private final MeterRegistry meterRegistry;

    public CapstoneSecurityConfig(JwtAuthFilter jwtAuthFilter,
                                  RateLimitProperties rateLimitProperties,
                                  MeterRegistry meterRegistry) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitProperties = rateLimitProperties;
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(rateLimitProperties, meterRegistry);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
