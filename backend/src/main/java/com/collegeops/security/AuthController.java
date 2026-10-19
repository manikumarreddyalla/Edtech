package com.collegeops.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate an institution user")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's profile")
    public AuthUserResponse me(JwtAuthenticationToken authentication) {
        UUID userId = UUID.fromString(authentication.getToken().getSubject());
        UUID institutionId = UUID.fromString(authentication.getToken().getClaimAsString("institution_id"));
        List<String> roles = authentication.getToken().getClaimAsStringList("roles");
        return authService.currentUser(userId, institutionId, roles);
    }
}
