package com.collegeops.accreditation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface EvidenceAssignmentRepository extends JpaRepository<EvidenceAssignment, UUID> {
    @Query("""
            select evidence from EvidenceAssignment evidence
            where evidence.institution.id = :institutionId
              and (:ownerUserId is null or evidence.ownerUserId = :ownerUserId)
              and (:academicYear is null or evidence.academicYear = :academicYear)
              and (:status is null or evidence.status = :status)
              and (:departmentId is null or evidence.department.id = :departmentId)
            """)
    Page<EvidenceAssignment> search(@Param("institutionId") UUID institutionId,
                                   @Param("ownerUserId") UUID ownerUserId,
                                   @Param("academicYear") String academicYear,
                                   @Param("status") EvidenceStatus status,
                                   @Param("departmentId") UUID departmentId,
                                   Pageable pageable);

    @Query("""
            select evidence from EvidenceAssignment evidence
            where evidence.institution.id = :institutionId
              and evidence.academicYear = :academicYear
              and evidence.status in :statuses
              and (:departmentId is null or evidence.department.id = :departmentId)
            """)
    Page<EvidenceAssignment> findMissing(@Param("institutionId") UUID institutionId,
                                        @Param("academicYear") String academicYear,
                                        @Param("departmentId") UUID departmentId,
                                        @Param("statuses") List<EvidenceStatus> statuses,
                                        Pageable pageable);

    Optional<EvidenceAssignment> findByIdAndInstitution_Id(UUID id, UUID institutionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select evidence from EvidenceAssignment evidence "
            + "where evidence.id = :id and evidence.institution.id = :institutionId")
    Optional<EvidenceAssignment> findForUpdateByIdAndInstitutionId(@Param("id") UUID id,
                                                                   @Param("institutionId") UUID institutionId);

    boolean existsByInstitution_IdAndCriterion_IdAndAcademicYearAndDepartment_Id(
            UUID institutionId, UUID criterionId, String academicYear, UUID departmentId);

    boolean existsByInstitution_IdAndCriterion_IdAndAcademicYearAndDepartmentIsNull(
            UUID institutionId, UUID criterionId, String academicYear);
}
