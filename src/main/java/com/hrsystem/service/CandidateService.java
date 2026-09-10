package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.dto.request.VacancyFilterDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CandidateService {

    Page<VacancyEntity> searchVacancies(VacancyFilterDto filter);

    VacancyEntity getVacancy(Long vacancyId);

    List<ApplicationEntity> listMyApplications(Long candidateProfileId);

    ApplicationEntity getMyApplication(Long candidateProfileId, Long applicationId);

    CandidateProfileEntity getProfile(Long candidateProfileId);

    CandidateProfileEntity updateProfile(Long candidateProfileId, String fullName, String targetTitle,
                                         String skills, String phone, String telegram, String portfolioLinks);
}
