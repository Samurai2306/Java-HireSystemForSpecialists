package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CandidateService {

    Page<VacancySummaryDto> searchVacancies(VacancyFilterDto filter);

    VacancyDetailsDto getVacancyDetails(Long vacancyId);

    ApplicationDto applyForVacancy(Long candidateUserId, ApplyVacancyDto dto);

    List<ApplicationDto> getMyApplications(Long candidateUserId);

    ApplicationDto getApplicationDetails(Long candidateUserId, Long applicationId);

    ApplicationDto withdrawApplication(Long candidateUserId, Long applicationId);

    CandidateProfileEntity getProfile(Long candidateUserId);

    CandidateProfileEntity updateProfile(Long candidateUserId, CandidateProfileUpdateDto dto);

    VacancyEntity getVacancy(Long vacancyId);

    List<ApplicationEntity> listMyApplications(Long candidateProfileId);

    ApplicationEntity getMyApplication(Long candidateProfileId, Long applicationId);

    CandidateProfileEntity updateProfile(Long candidateProfileId, String fullName, String targetTitle,
                                         String skills, String phone, String telegram, String portfolioLinks);
}
