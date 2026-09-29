package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.dto.request.CreateVacancyDto;
import com.hrsystem.dto.response.EmployerVacancyRowDto;

import java.util.List;

public interface EmployerService {

    VacancyEntity createVacancy(Long employerProfileId, CreateVacancyDto dto);

    List<EmployerVacancyRowDto> listMyVacancies(Long employerProfileId);

    VacancyEntity updateVacancy(Long employerProfileId, Long vacancyId, Integer salaryMin, Integer salaryMax, String requirements);

    VacancyEntity archiveVacancy(Long employerProfileId, Long vacancyId);

    VacancyEntity getOwnedVacancy(Long employerProfileId, Long vacancyId);

    EmployerProfileEntity getProfile(Long employerProfileId);

    EmployerProfileEntity updateProfile(Long employerProfileId, String companyName, String contactPerson,
                                        String websiteUrl, String description);

    List<ApplicationEntity> getApplicationsForVacancy(Long employerProfileId, Long vacancyId);
}
