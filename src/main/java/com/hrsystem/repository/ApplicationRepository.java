package com.hrsystem.repository;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    @EntityGraph(attributePaths = {"candidate", "vacancy"})
    List<ApplicationEntity> findByVacancyIdOrderByCreatedAtDesc(Long vacancyId);

    @EntityGraph(attributePaths = {"candidate", "vacancy"})
    List<ApplicationEntity> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"candidate", "vacancy", "vacancy.employer"})
    @Query("SELECT a FROM ApplicationEntity a WHERE a.id = :id")
    Optional<ApplicationEntity> findWithDetailsById(@Param("id") Long id);

    boolean existsByVacancyIdAndCandidateIdAndStatusIn(
            Long vacancyId,
            Long candidateId,
            Collection<ApplicationStatus> statuses
    );

    long countByVacancyId(Long vacancyId);
}
