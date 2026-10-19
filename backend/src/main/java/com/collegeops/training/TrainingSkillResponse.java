package com.collegeops.training;

import java.time.Instant;
import java.util.UUID;

public record TrainingSkillResponse(UUID id, UUID institutionId, String name, String description,
                                    Instant createdAt, Instant updatedAt) {
    static TrainingSkillResponse from(TrainingSkill skill) {
        return new TrainingSkillResponse(skill.getId(), skill.getInstitution().getId(), skill.getName(),
                skill.getDescription(), skill.getCreatedAt(), skill.getUpdatedAt());
    }
}
