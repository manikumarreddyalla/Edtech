package com.collegeops.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByInstitution_IdAndEmailIgnoreCase(UUID institutionId, String email);

    Optional<UserEntity> findByIdAndInstitution_Id(UUID id, UUID institutionId);

    boolean existsByInstitution_IdAndEmailIgnoreCase(UUID institutionId, String email);
}
