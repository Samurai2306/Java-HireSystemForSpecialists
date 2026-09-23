package com.hrsystem.repository;

import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VacancyRepository extends JpaRepository<VacancyEntity, Long> {

    /**
     * Каталог активных вакансий. Параметры-заглушки (ALL, 0, "") вместо null:
     * Hibernate 6 не типизирует NULL-параметры в PostgreSQL и запрос падает с "lower(bytea) does not exist".
     */
    @Query("""
            SELECT v FROM VacancyEntity v
            WHERE v.status = :status
              AND (:source = com.hrsystem.domain.enums.VacancySource.ALL OR v.sourceType = :source)
              AND (:minSalary <= 0 OR v.salaryMax >= :minSalary OR (v.salaryMin IS NOT NULL AND v.salaryMin >= :minSalary))
              AND (:keyword = '' OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.requirementsStack) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<VacancyEntity> findWithFilters(
            @Param("status") VacancyStatus status,
            @Param("source") VacancySource source,
            @Param("minSalary") Integer minSalary,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    boolean existsByContentHash(String contentHash);

    boolean existsBySourceUrl(String sourceUrl);

    List<VacancyEntity> findByEmployerIdOrderByPublishedAtDesc(Long employerId);

    @EntityGraph(attributePaths = "employer")
    @Query("SELECT v FROM VacancyEntity v WHERE v.id = :id")
    Optional<VacancyEntity> findWithEmployerById(@Param("id") Long id);

    long countByStatus(VacancyStatus status);

    long countByStatusAndSourceType(VacancyStatus status, VacancySource sourceType);

    List<VacancyEntity> findTop50ByOrderByPublishedAtDesc();

    @Query("""
            SELECT v FROM VacancyEntity v
            WHERE v.status = com.hrsystem.domain.enums.VacancyStatus.ACTIVE
              AND (:keyword = '' OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(v.requirementsStack, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:salaryMin <= 0 OR COALESCE(v.salaryMax, v.salaryMin) >= :salaryMin)
              AND (:sourceType = com.hrsystem.domain.enums.VacancySource.ALL OR v.sourceType = :sourceType)
            """)
    Page<VacancyEntity> searchActive(
            @Param("keyword") String keyword,
            @Param("salaryMin") Integer salaryMin,
            @Param("sourceType") VacancySource sourceType,
            Pageable pageable
    );
}
