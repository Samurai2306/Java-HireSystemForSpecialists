package com.hrsystem.repository;

import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployerProfileRepository extends JpaRepository<EmployerProfileEntity, Long> {
    Optional<EmployerProfileEntity> findByUserId(Long userId);
    Optional<EmployerProfileEntity> findByUser(UserEntity user);
}
