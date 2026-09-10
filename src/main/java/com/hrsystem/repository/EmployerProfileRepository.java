package com.hrsystem.repository;

import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfileEntity, Long> {
    Optional<EmployerProfileEntity> findByUser(UserEntity user);

    Optional<EmployerProfileEntity> findByUserId(Long userId);
}
