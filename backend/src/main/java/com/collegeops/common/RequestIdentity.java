package com.collegeops.common;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public record RequestIdentity(UUID userId, UUID institutionId) {
    public static RequestIdentity from(JwtAuthenticationToken authentication) {
        return new RequestIdentity(
                UUID.fromString(authentication.getToken().getSubject()),
                UUID.fromString(authentication.getToken().getClaimAsString("institution_id")));
    }
}
