package com.collegeops.security;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, AuthUserResponse user) {
}
