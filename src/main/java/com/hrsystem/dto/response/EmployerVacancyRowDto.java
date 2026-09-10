package com.hrsystem.dto.response;

import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.VacancyStatus;

public class EmployerVacancyRowDto {
    private Long id;
    private String title;
    private String publishedAt;
    private VacancyStatus status;
    private long applicationCount;

    public static EmployerVacancyRowDto from(VacancyEntity vacancy, long applicationCount) {
        EmployerVacancyRowDto dto = new EmployerVacancyRowDto();
        dto.id = vacancy.getId();
        dto.title = vacancy.getTitle();
        dto.publishedAt = vacancy.getPublishedAt() == null ? "" : vacancy.getPublishedAt().toString();
        dto.status = vacancy.getStatus();
        dto.applicationCount = applicationCount;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public VacancyStatus getStatus() {
        return status;
    }

    public long getApplicationCount() {
        return applicationCount;
    }
}
