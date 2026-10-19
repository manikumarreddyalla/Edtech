package com.collegeops.security;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(UUID id, UUID institutionId, String email, String displayName, List<String> roles) {
}
