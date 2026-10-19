package com.collegeops.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainingSkillRepository extends JpaRepository<TrainingSkill, UUID> {
    Page<TrainingSkill> findAllByInstitution_Id(UUID institutionId, Pageable pageable);
    boolean existsByInstitution_IdAndNormalizedName(UUID institutionId, String normalizedName);
}
