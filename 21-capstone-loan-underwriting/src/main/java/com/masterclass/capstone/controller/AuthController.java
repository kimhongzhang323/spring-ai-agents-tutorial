package com.masterclass.capstone.controller;

import com.masterclass.shared.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo-only auth endpoint. Issues a JWT for any of the hardcoded users.
 * In a real deployment, replace with your identity provider (Keycloak, Okta, etc.).
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Demo JWT issuance — not for production use")
public class AuthController {

    private static final Map<String, String> DEMO_USERS = Map.of(
            "officer1", "demo",
            "officer2", "demo",
            "admin",    "admin"
    );

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String username) {}

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for a JWT (demo only)")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String expected = DEMO_USERS.get(req.username());
        if (expected == null || !expected.equals(req.password())) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(req.username()), req.username()));
    }
}
