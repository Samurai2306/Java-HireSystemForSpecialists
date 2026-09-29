package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.domain.state.ApplicationStateMachine;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.exception.InvalidStateTransitionException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private VacancyRepository vacancyRepository;
    @Mock
    private CandidateProfileRepository candidateProfileRepository;
    @Mock
    private EmployerProfileRepository employerProfileRepository;

    private ApplicationServiceImpl service;
    private EmployerProfileEntity employer;
    private UserEntity employerUser;
    private VacancyEntity vacancy;
    private CandidateProfileEntity candidate;
    private ApplicationEntity application;

    @BeforeEach
    void setUp() {
        service = new ApplicationServiceImpl(
                applicationRepository,
                vacancyRepository,
                candidateProfileRepository,
                employerProfileRepository,
                new ApplicationStateMachine()
        );
        employerUser = new UserEntity();
        employerUser.setId(20L);
        employerUser.setRole(UserRole.EMPLOYER);

        employer = new EmployerProfileEntity();
        employer.setId(10L);
        employer.setUser(employerUser);

        vacancy = new VacancyEntity();
        vacancy.setId(100L);
        vacancy.setEmployer(employer);
        vacancy.setStatus(VacancyStatus.ACTIVE);

        UserEntity candidateUser = new UserEntity();
        candidateUser.setId(30L);
        candidate = new CandidateProfileEntity();
        candidate.setId(5L);
        candidate.setUser(candidateUser);

        application = new ApplicationEntity();
        application.setId(501L);
        application.setVacancy(vacancy);
        application.setCandidate(candidate);
        application.setStatus(ApplicationStatus.APPLIED);
    }

    @Test
    void employerCanMoveAppliedToReviewing() {
        when(applicationRepository.findWithDetailsById(501L)).thenReturn(Optional.of(application));
        when(employerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(employer));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationEntity updated = service.changeStatus(501L, ApplicationStatus.REVIEWING, "смотрим", 20L, UserRole.EMPLOYER);
        assertEquals(ApplicationStatus.REVIEWING, updated.getStatus());
        assertEquals("смотрим", updated.getStatusComment());
    }

    @Test
    void terminalOfferCannotReturnToApplied() {
        application.setStatus(ApplicationStatus.OFFER);
        when(applicationRepository.findWithDetailsById(501L)).thenReturn(Optional.of(application));
        when(employerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(employer));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.changeStatus(501L, ApplicationStatus.APPLIED, null, 20L, UserRole.EMPLOYER));
    }

    @Test
    void employerCannotChangeForeignApplication() {
        EmployerProfileEntity other = new EmployerProfileEntity();
        other.setId(77L);
        vacancy.setEmployer(other);
        when(applicationRepository.findWithDetailsById(501L)).thenReturn(Optional.of(application));
        when(employerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(employer));

        assertThrows(AccessDeniedException.class,
                () -> service.changeStatus(501L, ApplicationStatus.REVIEWING, null, 20L, UserRole.EMPLOYER));
    }

    @Test
    void candidateCanWithdrawFromApplied() {
        when(applicationRepository.findWithDetailsById(501L)).thenReturn(Optional.of(application));
        when(candidateProfileRepository.findByUserId(30L)).thenReturn(Optional.of(candidate));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationEntity updated = service.changeStatus(501L, ApplicationStatus.WITHDRAWN, "передумал", 30L, UserRole.CANDIDATE);
        assertEquals(ApplicationStatus.WITHDRAWN, updated.getStatus());
    }

    @Test
    void duplicateActiveApplicationIsBlocked() {
        when(candidateProfileRepository.findById(5L)).thenReturn(Optional.of(candidate));
        when(vacancyRepository.findById(100L)).thenReturn(Optional.of(vacancy));
        when(applicationRepository.existsByVacancyIdAndCandidateIdAndStatusIn(
                eq(100L), eq(5L), eq(EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWING))))
                .thenReturn(true);

        assertThrows(DuplicateApplicationException.class, () -> service.apply(5L, 100L, "hi"));
    }
}
