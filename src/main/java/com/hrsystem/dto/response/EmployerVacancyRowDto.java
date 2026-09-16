package com.hrsystem.dto.response;

import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancyStatus;

public record EmployerVacancyRowDto(
        Long id,
        String title,
        String publishedAt,
        VacancyStatus status,
        long applicationCount
) {
    public static EmployerVacancyRowDto from(VacancyEntity vacancy, long applicationCount) {
        return new EmployerVacancyRowDto(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getPublishedAt() == null ? "" : vacancy.getPublishedAt().toString(),
                vacancy.getStatus(),
                applicationCount
        );
    }
}
