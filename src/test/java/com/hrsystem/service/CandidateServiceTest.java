package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.*;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.exception.InvalidStateTransitionException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.impl.CandidateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    private CandidateService candidateService;

    private UserEntity candidateUser;
    private CandidateProfileEntity candidateProfile;
    private VacancyEntity testVacancy;

    @BeforeEach
    void setUp() {
        candidateService = new CandidateServiceImpl(vacancyRepository, applicationRepository, candidateProfileRepository);

        candidateUser = new UserEntity("damir@candidate.com", "hash", UserRole.CANDIDATE);
        candidateUser.setId(1L);

        candidateProfile = new CandidateProfileEntity(candidateUser, "Дамир Галимов", "Java Developer", "Java, Spring", "+79991112233", "@damir");
        candidateProfile.setId(10L);

        testVacancy = new VacancyEntity("Senior Java Developer", "Яндекс", 250000, 320000,
                Currency.RUB, "Описание", "Java 17, Spring Boot", "Москва",
                EmploymentType.REMOTE, VacancySource.WEBSITE);
        testVacancy.setId(100L);
        testVacancy.setStatus(VacancyStatus.ACTIVE);
    }

    @Test
    @DisplayName("DoD-3 & DoD-4: Пагинация по 10 записей и фильтрация вакансий по стеку и зарплате")
    void searchVacancies_withPaginationAndFilters() {
        VacancyFilterDto filter = new VacancyFilterDto("Java", 200000, VacancySource.WEBSITE, 1, 10);

        List<VacancyEntity> list = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            VacancyEntity v = new VacancyEntity("Java Developer #" + i, "Company", 220000, 260000,
                    Currency.RUB, "Desc", "Java", "City", EmploymentType.REMOTE, VacancySource.WEBSITE);
            v.setId(i);
            list.add(v);
        }
        Page<VacancyEntity> page = new PageImpl<>(list, PageRequest.of(0, 10), 15);

        when(vacancyRepository.findWithFilters(
                eq(VacancyStatus.ACTIVE),
                eq(VacancySource.WEBSITE),
                eq(200000),
                eq("Java"),
                any(Pageable.class)
        )).thenReturn(page);

        Page<VacancySummaryDto> result = candidateService.searchVacancies(filter);

        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTitle()).contains("Java");
        assertThat(result.getContent().get(0).getSourceFormatted()).isEqualTo("[Сайт]");
    }

    @Test
    @DisplayName("Просмотр детальной информации о вакансии")
    void getVacancyDetails_shouldReturnFullDto() {
        when(vacancyRepository.findById(100L)).thenReturn(Optional.of(testVacancy));

        VacancyDetailsDto details = candidateService.getVacancyDetails(100L);

        assertThat(details).isNotNull();
        assertThat(details.getId()).isEqualTo(100L);
        assertThat(details.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(details.getCompanyName()).isEqualTo("Яндекс");
        assertThat(details.getRequirementsStack()).isEqualTo("Java 17, Spring Boot");
    }

    @Test
    @DisplayName("Успешная подача первого отклика на вакансию")
    void applyForVacancy_successfulSubmission() {
        ApplyVacancyDto dto = new ApplyVacancyDto(100L, "Хочу работать у вас!", false);

        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(vacancyRepository.findById(100L)).thenReturn(Optional.of(testVacancy));
        when(applicationRepository.existsActiveApplication(eq(100L), eq(10L), anyCollection())).thenReturn(false);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(inv -> {
            ApplicationEntity a = inv.getArgument(0);
            a.setId(500L);
            return a;
        });

        ApplicationDto result = candidateService.applyForVacancy(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getVacancyTitle()).isEqualTo("Senior Java Developer");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("DoD-5: Повторный отклик на ту же вакансию блокируется с DuplicateApplicationException")
    void applyForVacancy_shouldBlockDuplicateActiveApplication() {
        ApplyVacancyDto dto = new ApplyVacancyDto(100L, "Повторный отклик", false);

        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(vacancyRepository.findById(100L)).thenReturn(Optional.of(testVacancy));
        when(applicationRepository.existsActiveApplication(eq(100L), eq(10L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> candidateService.applyForVacancy(1L, dto))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Повторный отклик запрещен");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("DoD-6: Успешный отзыв активного отклика переводит статус в WITHDRAWN")
    void withdrawApplication_shouldTransitionToWithdrawn() {
        ApplicationEntity app = new ApplicationEntity(testVacancy, candidateProfile, "Письмо");
        app.setId(777L);
        app.setStatus(ApplicationStatus.APPLIED);

        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(applicationRepository.findById(777L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationDto result = candidateService.withdrawApplication(1L, 777L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(app.getStatusComment()).contains("отозван соискателем");
    }

    @Test
    @DisplayName("DoD-6: Попытка отозвать уже завершенный отклик (OFFER/REJECTED) отклоняется")
    void withdrawApplication_terminalStatus_shouldThrowException() {
        ApplicationEntity app = new ApplicationEntity(testVacancy, candidateProfile, "Письмо");
        app.setId(777L);
        app.setStatus(ApplicationStatus.OFFER);

        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(applicationRepository.findById(777L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> candidateService.withdrawApplication(1L, 777L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("является финальным");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Попытка отозвать чужой отклик отклоняется с AccessDeniedException")
    void withdrawApplication_foreignApplication_shouldThrowAccessDenied() {
        CandidateProfileEntity otherCandidate = new CandidateProfileEntity();
        otherCandidate.setId(999L);

        ApplicationEntity app = new ApplicationEntity(testVacancy, otherCandidate, "Чужое письмо");
        app.setId(777L);
        app.setStatus(ApplicationStatus.APPLIED);

        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(applicationRepository.findById(777L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> candidateService.withdrawApplication(1L, 777L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("только свои собственные отклики");
    }

    @Test
    @DisplayName("Редактирование профиля соискателя")
    void updateProfile_shouldUpdateFields() {
        when(candidateProfileRepository.findByUserId(1L)).thenReturn(Optional.of(candidateProfile));
        when(candidateProfileRepository.save(any(CandidateProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateProfileUpdateDto dto = new CandidateProfileUpdateDto(
                "Дамир Галимов Обновленный",
                "Lead Java Architect",
                "Java, Spring Cloud, Kafka",
                "+78889990011",
                "@damir_lead",
                "https://github.com/damir-new"
        );

        CandidateProfileEntity updated = candidateService.updateProfile(1L, dto);

        assertThat(updated.getFullName()).isEqualTo("Дамир Галимов Обновленный");
        assertThat(updated.getTargetTitle()).isEqualTo("Lead Java Architect");
        assertThat(updated.getSkills()).isEqualTo("Java, Spring Cloud, Kafka");
        assertThat(updated.getPhone()).isEqualTo("+78889990011");
        assertThat(updated.getTelegram()).isEqualTo("@damir_lead");
    }
}
