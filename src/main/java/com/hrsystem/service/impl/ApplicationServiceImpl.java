package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.domain.state.ApplicationStateMachine;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.ApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final Set<ApplicationStatus> ACTIVE_STATUSES =
            EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWING);
    private static final Set<ApplicationStatus> EMPLOYER_TARGETS =
            EnumSet.of(ApplicationStatus.REVIEWING, ApplicationStatus.OFFER, ApplicationStatus.REJECTED);

    private final ApplicationRepository applicationRepository;
    private final VacancyRepository vacancyRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final ApplicationStateMachine stateMachine;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  VacancyRepository vacancyRepository,
                                  CandidateProfileRepository candidateProfileRepository,
                                  EmployerProfileRepository employerProfileRepository,
                                  ApplicationStateMachine stateMachine) {
        this.applicationRepository = applicationRepository;
        this.vacancyRepository = vacancyRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.stateMachine = stateMachine;
    }

    @Override
    public ApplicationEntity changeStatus(Long applicationId, ApplicationStatus targetStatus, String comment,
                                          Long actorUserId, UserRole actorRole) {
        ApplicationEntity application = applicationRepository.findWithDetailsById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Отклик #" + applicationId + " не найден"));

        if (actorRole == UserRole.CANDIDATE) {
            CandidateProfileEntity profile = candidateProfileRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new AccessDeniedException("Профиль соискателя не найден"));
            if (!profile.getId().equals(application.getCandidate().getId())) {
                throw new AccessDeniedException("Нельзя изменять чужой отклик");
            }
        } else if (actorRole == UserRole.EMPLOYER) {
            EmployerProfileEntity employer = employerProfileRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new AccessDeniedException("Профиль работодателя не найден"));
            VacancyEntity vacancy = application.getVacancy();
            if (vacancy.getEmployer() == null || !employer.getId().equals(vacancy.getEmployer().getId())) {
                throw new AccessDeniedException("Нельзя менять статус отклика на чужую вакансию");
            }
        } else {
            throw new AccessDeniedException("Смена статуса отклика недоступна для роли " + actorRole);
        }

        stateMachine.validate(application.getStatus(), targetStatus);

        if (actorRole == UserRole.CANDIDATE && targetStatus != ApplicationStatus.WITHDRAWN) {
            throw new AccessDeniedException("Соискатель может только отозвать свой отклик");
        }
        if (actorRole == UserRole.EMPLOYER && !EMPLOYER_TARGETS.contains(targetStatus)) {
            throw new AccessDeniedException("Работодатель не может установить статус " + targetStatus);
        }
        application.setStatus(targetStatus);
        if (comment != null && !comment.isBlank()) {
            application.setStatusComment(comment.trim());
        }
        return applicationRepository.save(application);
    }

    @Override
    public ApplicationEntity apply(Long candidateProfileId, Long vacancyId, String coverLetter) {
        CandidateProfileEntity candidate = candidateProfileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден"));
        VacancyEntity vacancy = vacancyRepository.findById(vacancyId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия #" + vacancyId + " не найдена"));
        if (vacancy.getStatus() != VacancyStatus.ACTIVE) {
            throw new IllegalArgumentException("Отклик возможен только на активную вакансию");
        }
        if (applicationRepository.existsByVacancyIdAndCandidateIdAndStatusIn(
                vacancyId, candidateProfileId, ACTIVE_STATUSES)) {
            throw new DuplicateApplicationException(
                    "Уже есть активный отклик на вакансию #" + vacancyId + " (APPLIED или REVIEWING)");
        }
        ApplicationEntity application = new ApplicationEntity();
        application.setCandidate(candidate);
        application.setVacancy(vacancy);
        application.setCoverLetter(coverLetter == null || coverLetter.isBlank() ? candidate.getSkills() : coverLetter.trim());
        application.setStatus(ApplicationStatus.APPLIED);
        return applicationRepository.save(application);
    }

    @Override
    public ApplicationEntity withdraw(Long candidateProfileId, Long applicationId) {
        CandidateProfileEntity candidate = candidateProfileRepository.findById(candidateProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден"));
        return changeStatus(applicationId, ApplicationStatus.WITHDRAWN, "Отзыв соискателем",
                candidate.getUser().getId(), UserRole.CANDIDATE);
    }
}
