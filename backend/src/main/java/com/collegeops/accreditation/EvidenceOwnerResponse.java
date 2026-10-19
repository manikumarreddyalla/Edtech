package com.collegeops.accreditation;

import com.collegeops.users.UserEntity;

import java.util.Set;
import java.util.UUID;

public record EvidenceOwnerResponse(UUID id, String displayName, Set<String> roles) {
    static EvidenceOwnerResponse from(UserEntity user) {
        return new EvidenceOwnerResponse(user.getId(), user.getDisplayName(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
