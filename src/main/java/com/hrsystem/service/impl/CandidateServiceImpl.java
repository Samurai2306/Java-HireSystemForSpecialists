package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.exception.InvalidStateTransitionException;
import com.hrsystem.exception.ValidationException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.CandidateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidateServiceImpl implements CandidateService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

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
    public Page<VacancySummaryDto> searchVacancies(VacancyFilterDto filter) {
        if (filter == null) {
            filter = new VacancyFilterDto();
        }

        Pageable pageable = PageRequest.of(
                filter.getPageNumber() - 1,
                filter.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt")
        );

        // Заглушки вместо null: NULL-параметры в PostgreSQL ломают типизацию запроса.
        VacancySource sourceParam = filter.getSource() != null ? filter.getSource() : VacancySource.ALL;
        String keywordParam = filter.hasKeyword() ? filter.getKeyword().trim() : "";
        int minSalaryParam = filter.hasSalaryMin() ? filter.getSalaryMin() : 0;

        Page<VacancyEntity> entityPage = vacancyRepository.findWithFilters(
                VacancyStatus.ACTIVE,
                sourceParam,
                minSalaryParam,
                keywordParam,
                pageable
        );

        return entityPage.map(this::mapToSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyDetailsDto getVacancyDetails(Long vacancyId) {
        if (vacancyId == null) {
            throw new ValidationException("ID вакансии не может быть null.");
        }

        return mapToDetailsDto(findVacancy(vacancyId));
    }

    @Override
    @Transactional
    public ApplicationDto applyForVacancy(Long candidateUserId, ApplyVacancyDto dto) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }
        if (dto == null || dto.getVacancyId() == null) {
            throw new ValidationException("Не указан ID вакансии для отклика.");
        }

        CandidateProfileEntity candidate = findCandidate(candidateUserId);

        VacancyEntity vacancy = findVacancy(dto.getVacancyId());

        if (vacancy.getStatus() != VacancyStatus.ACTIVE) {
            throw new ValidationException("Невозможно подать отклик: вакансия не активна или находится в архиве.");
        }

        // Check for active duplicate applications
        boolean hasActiveApplication = applicationRepository.existsActiveApplication(
                vacancy.getId(),
                candidate.getId(),
                List.of(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWING)
        );

        if (hasActiveApplication) {
            throw new DuplicateApplicationException("Вы уже подали заявку на данную вакансию, которая находится на рассмотрении. Повторный отклик запрещен.");
        }

        String letter = dto.getCoverLetter();
        if (dto.isUseProfileDefault() || letter == null || letter.trim().isEmpty()) {
            letter = generateDefaultCoverLetter(candidate, vacancy);
        } else {
            letter = letter.trim();
        }

        ApplicationEntity application = new ApplicationEntity(vacancy, candidate, letter);
        application.setStatus(ApplicationStatus.APPLIED);
        ApplicationEntity saved = applicationRepository.save(application);

        return mapToApplicationDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDto> getMyApplications(Long candidateUserId) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }

        CandidateProfileEntity candidate = findCandidate(candidateUserId);

        List<ApplicationEntity> list = applicationRepository.findByCandidateProfileIdOrderByCreatedAtDesc(candidate.getId());
        return list.stream().map(this::mapToApplicationDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getApplicationDetails(Long candidateUserId, Long applicationId) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }
        if (applicationId == null) {
            throw new ValidationException("ID отклика не указан.");
        }

        CandidateProfileEntity candidate = findCandidate(candidateUserId);

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Отклик с ID " + applicationId + " не найден."));

        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new AccessDeniedException("Вы можете просматривать только свои собственные отклики.");
        }

        return mapToApplicationDto(application);
    }

    @Override
    @Transactional
    public ApplicationDto withdrawApplication(Long candidateUserId, Long applicationId) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }
        if (applicationId == null) {
            throw new ValidationException("ID отклика не указан.");
        }

        CandidateProfileEntity candidate = findCandidate(candidateUserId);

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Отклик с ID " + applicationId + " не найден."));

        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new AccessDeniedException("Вы можете отзывать только свои собственные отклики.");
        }

        ApplicationStatus currentStatus = application.getStatus();
        if (currentStatus != ApplicationStatus.APPLIED && currentStatus != ApplicationStatus.REVIEWING) {
            throw new InvalidStateTransitionException(
                    String.format("Отзыв отклика невозможен: текущий статус '%s' является финальным.", currentStatus)
            );
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setStatusComment("Отклик отозван соискателем.");
        ApplicationEntity updated = applicationRepository.save(application);

        return mapToApplicationDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileEntity getProfile(Long candidateUserId) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }
        return findCandidate(candidateUserId);
    }

    @Override
    @Transactional
    public CandidateProfileEntity updateProfile(Long candidateUserId, CandidateProfileUpdateDto dto) {
        if (candidateUserId == null) {
            throw new ValidationException("Пользователь не авторизован.");
        }
        if (dto == null) {
            throw new ValidationException("Данные для обновления профиля не переданы.");
        }

        CandidateProfileEntity profile = findCandidate(candidateUserId);

        if (dto.getFullName() != null && !dto.getFullName().trim().isEmpty()) {
            profile.setFullName(dto.getFullName().trim());
        }
        if (dto.getTargetTitle() != null) {
            profile.setTargetTitle(dto.getTargetTitle().trim());
        }
        if (dto.getSkills() != null) {
            profile.setSkills(dto.getSkills().trim());
        }
        if (dto.getPhone() != null) {
            profile.setPhone(dto.getPhone().trim());
        }
        if (dto.getTelegram() != null) {
            profile.setTelegram(dto.getTelegram().trim());
        }
        if (dto.getPortfolioLinks() != null) {
            profile.setPortfolioLinks(dto.getPortfolioLinks().trim());
        }

        return candidateProfileRepository.save(profile);
    }

    private CandidateProfileEntity findCandidate(Long candidateUserId) {
        return candidateProfileRepository.findByUserId(candidateUserId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден."));
    }

    private VacancyEntity findVacancy(Long vacancyId) {
        return vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия с ID " + vacancyId + " не найдена."));
    }

    private String generateDefaultCoverLetter(CandidateProfileEntity candidate, VacancyEntity vacancy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте! Меня зовут ").append(candidate.getFullName()).append(".\n");
        sb.append("Меня заинтересовала позиция '").append(vacancy.getTitle()).append("' в компании '").append(vacancy.getCompanyName()).append("'.\n");
        if (candidate.getTargetTitle() != null && !candidate.getTargetTitle().isEmpty()) {
            sb.append("Моя специализация: ").append(candidate.getTargetTitle()).append(".\n");
        }
        if (candidate.getSkills() != null && !candidate.getSkills().isEmpty()) {
            sb.append("Ключевые навыки и стек: ").append(candidate.getSkills()).append(".\n");
        }
        if (candidate.getTelegram() != null && !candidate.getTelegram().isEmpty()) {
            sb.append("Telegram для связи: ").append(candidate.getTelegram()).append("\n");
        }
        if (candidate.getPhone() != null && !candidate.getPhone().isEmpty()) {
            sb.append("Телефон: ").append(candidate.getPhone()).append("\n");
        }
        sb.append("Буду рад обсудить подробности на интервью.");
        return sb.toString();
    }

    private VacancySummaryDto mapToSummaryDto(VacancyEntity entity) {
        return new VacancySummaryDto(
                entity.getId(),
                entity.getTitle(),
                entity.getCompanyName(),
                entity.formatSalary(),
                entity.getSourceType(),
                entity.formatPublishedDate()
        );
    }

    private VacancyDetailsDto mapToDetailsDto(VacancyEntity entity) {
        VacancyDetailsDto dto = new VacancyDetailsDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCompanyName(entity.getCompanyName());
        dto.setSalaryMin(entity.getSalaryMin());
        dto.setSalaryMax(entity.getSalaryMax());
        dto.setCurrency(entity.getCurrency());
        dto.setSalaryFormatted(entity.formatSalary());
        dto.setDescription(entity.getDescription());
        dto.setRequirementsStack(entity.getRequirementsStack());
        dto.setLocation(entity.getLocation());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceUrl(entity.getSourceUrl());
        dto.setPublishedDateFormatted(entity.formatPublishedDate());
        return dto;
    }

    private ApplicationDto mapToApplicationDto(ApplicationEntity entity) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(entity.getId());
        if (entity.getVacancy() != null) {
            dto.setVacancyId(entity.getVacancy().getId());
            dto.setVacancyTitle(entity.getVacancy().getTitle());
            dto.setCompanyName(entity.getVacancy().getCompanyName());
        }
        if (entity.getCandidate() != null) {
            dto.setCandidateId(entity.getCandidate().getId());
            dto.setCandidateFullName(entity.getCandidate().getFullName());
        }
        dto.setCoverLetter(entity.getCoverLetter());
        dto.setStatus(entity.getStatus());
        dto.setStatusComment(entity.getStatusComment());
        dto.setAppliedAtFormatted(entity.formatCreatedAt());
        dto.setUpdatedAtFormatted(entity.getUpdatedAt() != null ?
                DATE_TIME_FORMATTER.format(entity.getUpdatedAt()) : "-");
        return dto;
    }
}
