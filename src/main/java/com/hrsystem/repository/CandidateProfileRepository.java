package com.hrsystem.repository;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfileEntity, Long> {
    Optional<CandidateProfileEntity> findByUser(UserEntity user);

    Optional<CandidateProfileEntity> findByUserId(Long userId);
}
