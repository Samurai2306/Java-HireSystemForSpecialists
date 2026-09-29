package com.hrsystem.repository;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    @EntityGraph(attributePaths = {"candidate", "vacancy"})
    List<ApplicationEntity> findByVacancyIdOrderByCreatedAtDesc(Long vacancyId);

    @EntityGraph(attributePaths = {"candidate", "vacancy"})
    List<ApplicationEntity> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"candidate", "vacancy"})
    @Query("SELECT a FROM ApplicationEntity a WHERE a.candidate.id = :candidateProfileId ORDER BY a.createdAt DESC")
    List<ApplicationEntity> findByCandidateProfileIdOrderByCreatedAtDesc(@Param("candidateProfileId") Long candidateProfileId);

    @EntityGraph(attributePaths = {"candidate", "vacancy", "vacancy.employer"})
    @Query("SELECT a FROM ApplicationEntity a WHERE a.id = :id")
    Optional<ApplicationEntity> findWithDetailsById(@Param("id") Long id);

    boolean existsByVacancyIdAndCandidateIdAndStatusIn(
            Long vacancyId,
            Long candidateId,
            Collection<ApplicationStatus> statuses
    );

    @Query("SELECT COUNT(a) > 0 FROM ApplicationEntity a " +
           "WHERE a.vacancy.id = :vacancyId " +
           "AND a.candidate.id = :candidateProfileId " +
           "AND a.status IN :statuses")
    boolean existsActiveApplication(
            @Param("vacancyId") Long vacancyId,
            @Param("candidateProfileId") Long candidateProfileId,
            @Param("statuses") Collection<ApplicationStatus> statuses
    );

    long countByVacancyId(Long vacancyId);
}
