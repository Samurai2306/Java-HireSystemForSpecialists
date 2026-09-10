package com.hrsystem.repository;

import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.enums.VacancySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParsingSourceRepository extends JpaRepository<ParsingSourceEntity, Long> {

    List<ParsingSourceEntity> findByActiveTrueAndSourceType(VacancySource sourceType);

    List<ParsingSourceEntity> findByActiveTrue();

    List<ParsingSourceEntity> findAllByOrderByIdAsc();

    Optional<ParsingSourceEntity> findByBaseUrlIgnoreCase(String baseUrl);

    boolean existsByBaseUrlIgnoreCase(String baseUrl);
}
