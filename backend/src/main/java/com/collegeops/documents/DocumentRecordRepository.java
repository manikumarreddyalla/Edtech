package com.collegeops.documents;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, UUID> {
    @Query("""
            select document from DocumentRecord document
            where document.institution.id = :institutionId
              and (:ownerUserId is null or document.ownerUserId = :ownerUserId)
              and (:query is null or lower(document.title) like lower(concat('%', :query, '%')))
              and (:category is null or document.category = :category)
              and (:departmentId is null or document.department.id = :departmentId)
            """)
    Page<DocumentRecord> search(@Param("institutionId") UUID institutionId,
                                @Param("ownerUserId") UUID ownerUserId,
                                @Param("query") String query,
                                @Param("category") String category,
                                @Param("departmentId") UUID departmentId,
                                Pageable pageable);

    Optional<DocumentRecord> findByIdAndInstitution_Id(UUID id, UUID institutionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from DocumentRecord document "
            + "where document.id = :id and document.institution.id = :institutionId")
    Optional<DocumentRecord> findForUpdateByIdAndInstitutionId(@Param("id") UUID id,
                                                                                @Param("institutionId") UUID institutionId);
}
