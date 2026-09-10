package com.hrsystem.repository;

import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VacancyRepository extends JpaRepository<VacancyEntity, Long> {

    boolean existsByContentHash(String contentHash);

    boolean existsBySourceUrl(String sourceUrl);

    List<VacancyEntity> findByEmployerIdOrderByPublishedAtDesc(Long employerId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "employer")
    @Query("SELECT v FROM VacancyEntity v WHERE v.id = :id")
    java.util.Optional<VacancyEntity> findWithEmployerById(@Param("id") Long id);

    long countByStatus(VacancyStatus status);

    long countByStatusAndSourceType(VacancyStatus status, VacancySource sourceType);

    List<VacancyEntity> findTop50ByOrderByPublishedAtDesc();

    @Query("""
            SELECT v FROM VacancyEntity v
            WHERE v.status = com.hrsystem.domain.enums.VacancyStatus.ACTIVE
              AND (:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(v.requirementsStack, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:salaryMin IS NULL OR COALESCE(v.salaryMax, v.salaryMin) >= :salaryMin)
              AND (:sourceType IS NULL OR v.sourceType = :sourceType)
            """)
    Page<VacancyEntity> searchActive(
            @Param("keyword") String keyword,
            @Param("salaryMin") Integer salaryMin,
            @Param("sourceType") VacancySource sourceType,
            Pageable pageable
    );
}
