package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.CandidateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CandidateServiceImpl implements CandidateService {

    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;

    public CandidateServiceImpl(VacancyRepository vacancyRepository,
                                ApplicationRepository applicationRepository,
                                CandidateProfileRepository candidateProfileRepository) {
        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
        this.candidateProfileRepository = candidateProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VacancyEntity> searchVacancies(VacancyFilterDto filter) {
        String keyword = filter.getKeyword() == null || filter.getKeyword().isBlank()
                ? null
                : filter.getKeyword().trim();
        return vacancyRepository.searchActive(
                keyword,
                filter.getSalaryMin(),
                filter.getSourceType(),
                PageRequest.of(filter.getPage(), filter.getPageSize(), Sort.by(Sort.Direction.DESC, "publishedAt"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyEntity getVacancy(Long vacancyId) {
        return vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия #" + vacancyId + " не найдена"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationEntity> listMyApplications(Long candidateProfileId) {
        return applicationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateProfileId);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationEntity getMyApplication(Long candidateProfileId, Long applicationId) {
        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Отклик #" + applicationId + " не найден"));
        if (!candidateProfileId.equals(application.getCandidate().getId())) {
            throw new AccessDeniedException("Это не ваш отклик");
        }
        return application;
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileEntity getProfile(Long candidateProfileId) {
        return candidateProfileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден"));
    }

    @Override
    public CandidateProfileEntity updateProfile(Long candidateProfileId, String fullName, String targetTitle,
                                                String skills, String phone, String telegram, String portfolioLinks) {
        CandidateProfileEntity profile = getProfile(candidateProfileId);
        if (fullName != null && !fullName.isBlank()) {
            profile.setFullName(fullName.trim());
        }
        if (targetTitle != null) {
            profile.setTargetTitle(targetTitle.isBlank() ? null : targetTitle.trim());
        }
        if (skills != null) {
            profile.setSkills(skills.isBlank() ? null : skills.trim());
        }
        if (phone != null) {
            profile.setPhone(phone.isBlank() ? null : phone.trim());
        }
        if (telegram != null) {
            profile.setTelegram(telegram.isBlank() ? null : telegram.trim());
        }
        if (portfolioLinks != null) {
            profile.setPortfolioLinks(portfolioLinks.isBlank() ? null : portfolioLinks.trim());
        }
        return candidateProfileRepository.save(profile);
    }
}
