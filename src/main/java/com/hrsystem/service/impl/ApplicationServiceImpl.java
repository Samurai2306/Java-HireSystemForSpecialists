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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

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
        Optional<ApplicationEntity> found = applicationRepository.findWithDetailsById(applicationId);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Отклик #" + applicationId + " не найден");
        }
        ApplicationEntity application = found.get();

        checkOwner(application, actorUserId, actorRole);
        stateMachine.validate(application.getStatus(), targetStatus);
        checkRoleCanSetStatus(actorRole, targetStatus);

        application.setStatus(targetStatus);
        if (comment != null && !comment.isBlank()) {
            application.setStatusComment(comment.trim());
        }
        return applicationRepository.save(application);
    }

    @Override
    public ApplicationEntity apply(Long candidateProfileId, Long vacancyId, String coverLetter) {
        Optional<CandidateProfileEntity> candidateFound = candidateProfileRepository.findById(candidateProfileId);
        if (candidateFound.isEmpty()) {
            throw new EntityNotFoundException("Профиль соискателя не найден");
        }
        Optional<VacancyEntity> vacancyFound = vacancyRepository.findById(vacancyId);
        if (vacancyFound.isEmpty()) {
            throw new EntityNotFoundException("Вакансия #" + vacancyId + " не найдена");
        }
        CandidateProfileEntity candidate = candidateFound.get();
        VacancyEntity vacancy = vacancyFound.get();
        if (vacancy.getStatus() != VacancyStatus.ACTIVE) {
            throw new IllegalArgumentException("Отклик возможен только на активную вакансию");
        }

        List<ApplicationStatus> activeStatuses = new ArrayList<>();
        activeStatuses.add(ApplicationStatus.APPLIED);
        activeStatuses.add(ApplicationStatus.REVIEWING);
        if (applicationRepository.existsByVacancyIdAndCandidateIdAndStatusIn(
                vacancyId, candidateProfileId, activeStatuses)) {
            throw new DuplicateApplicationException(
                    "Уже есть активный отклик на вакансию #" + vacancyId + " (APPLIED или REVIEWING)");
        }

        ApplicationEntity application = new ApplicationEntity();
        application.setCandidate(candidate);
        application.setVacancy(vacancy);
        if (coverLetter == null || coverLetter.isBlank()) {
            application.setCoverLetter(candidate.getSkills());
        } else {
            application.setCoverLetter(coverLetter.trim());
        }
        application.setStatus(ApplicationStatus.APPLIED);
        return applicationRepository.save(application);
    }

    @Override
    public ApplicationEntity withdraw(Long candidateProfileId, Long applicationId) {
        Optional<CandidateProfileEntity> found = candidateProfileRepository.findById(candidateProfileId);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Профиль соискателя не найден");
        }
        return changeStatus(applicationId, ApplicationStatus.WITHDRAWN, "Отзыв соискателем",
                found.get().getUser().getId(), UserRole.CANDIDATE);
    }

    private void checkOwner(ApplicationEntity application, Long actorUserId, UserRole actorRole) {
        if (actorRole == UserRole.CANDIDATE) {
            Optional<CandidateProfileEntity> found = candidateProfileRepository.findByUserId(actorUserId);
            if (found.isEmpty()) {
                throw new AccessDeniedException("Профиль соискателя не найден");
            }
            if (!found.get().getId().equals(application.getCandidate().getId())) {
                throw new AccessDeniedException("Нельзя изменять чужой отклик");
            }
            return;
        }
        if (actorRole == UserRole.EMPLOYER) {
            Optional<EmployerProfileEntity> found = employerProfileRepository.findByUserId(actorUserId);
            if (found.isEmpty()) {
                throw new AccessDeniedException("Профиль работодателя не найден");
            }
            VacancyEntity vacancy = application.getVacancy();
            if (vacancy.getEmployer() == null || !found.get().getId().equals(vacancy.getEmployer().getId())) {
                throw new AccessDeniedException("Нельзя менять статус отклика на чужую вакансию");
            }
            return;
        }
        throw new AccessDeniedException("Смена статуса отклика недоступна для роли " + actorRole);
    }

    private void checkRoleCanSetStatus(UserRole actorRole, ApplicationStatus targetStatus) {
        if (actorRole == UserRole.CANDIDATE && targetStatus != ApplicationStatus.WITHDRAWN) {
            throw new AccessDeniedException("Соискатель может только отозвать свой отклик");
        }
        if (actorRole == UserRole.EMPLOYER) {
            boolean allowed = targetStatus == ApplicationStatus.REVIEWING
                    || targetStatus == ApplicationStatus.OFFER
                    || targetStatus == ApplicationStatus.REJECTED;
            if (!allowed) {
                throw new AccessDeniedException("Работодатель не может установить статус " + targetStatus);
            }
        }
    }
}
