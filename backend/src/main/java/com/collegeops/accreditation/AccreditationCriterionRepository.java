package com.collegeops.accreditation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccreditationCriterionRepository extends JpaRepository<AccreditationCriterion, UUID> {
    @Query("""
            select criterion from AccreditationCriterion criterion
            where criterion.institution.id = :institutionId
              and (:frameworkId is null or criterion.framework.id = :frameworkId)
            """)
    Page<AccreditationCriterion> search(@Param("institutionId") UUID institutionId,
                                        @Param("frameworkId") UUID frameworkId,
                                        Pageable pageable);

    Optional<AccreditationCriterion> findByIdAndInstitution_Id(UUID id, UUID institutionId);

    boolean existsByFramework_IdAndCodeIgnoreCase(UUID frameworkId, String code);
}
