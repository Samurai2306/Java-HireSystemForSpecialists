package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;

public record CreateVacancyDto(
        String title,
        Integer salaryMin,
        Integer salaryMax,
        Currency currency,
        String requirementsStack,
        String description,
        EmploymentType employmentType,
        String location
) {
}
