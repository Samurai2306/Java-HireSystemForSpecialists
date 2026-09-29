package com.hrsystem.repository;

import com.hrsystem.domain.entity.ParsingLogEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParsingLogRepository extends JpaRepository<ParsingLogEntity, Long> {

    @EntityGraph(attributePaths = "source")
    List<ParsingLogEntity> findTop20ByOrderByStartedAtDesc();

    Optional<ParsingLogEntity> findTopByOrderByFinishedAtDesc();

    Optional<ParsingLogEntity> findTopByOrderByStartedAtDesc();
}
