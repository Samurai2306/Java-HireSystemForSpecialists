package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.request.CreateVacancyDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployerServiceImplTest {

    @Mock
    private EmployerProfileRepository employerProfileRepository;
    @Mock
    private VacancyRepository vacancyRepository;
    @Mock
    private ApplicationRepository applicationRepository;

    private EmployerServiceImpl service;
    private EmployerProfileEntity employer;

    @BeforeEach
    void setUp() {
        service = new EmployerServiceImpl(employerProfileRepository, vacancyRepository, applicationRepository);
        employer = new EmployerProfileEntity();
        employer.setId(10L);
        employer.setCompanyName("TechNova");
        lenient().when(employerProfileRepository.findById(10L)).thenReturn(Optional.of(employer));
    }

    @Test
    void createVacancySetsManualActiveAndNotParsed() {
        when(vacancyRepository.save(any(VacancyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateVacancyDto dto = new CreateVacancyDto();
        dto.setTitle("Java Backend Developer");
        dto.setSalaryMin(200_000);
        dto.setSalaryMax(280_000);
        dto.setCurrency(Currency.RUB);
        dto.setDescription("Spring Boot, PostgreSQL");
        dto.setRequirementsStack("Java, Spring");
        dto.setEmploymentType(EmploymentType.REMOTE);

        VacancyEntity created = service.createVacancy(10L, dto);

        assertEquals(VacancySource.MANUAL, created.getSourceType());
        assertEquals(VacancyStatus.ACTIVE, created.getStatus());
        assertFalse(created.isParsed());
        assertEquals("TechNova", created.getCompanyName());
        assertEquals(employer, created.getEmployer());
        verify(vacancyRepository).save(any(VacancyEntity.class));
    }

    @Test
    void archiveVacancyChangesStatus() {
        VacancyEntity vacancy = ownedVacancy(5L);
        when(vacancyRepository.findWithEmployerById(5L)).thenReturn(Optional.of(vacancy));
        when(vacancyRepository.save(any(VacancyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VacancyEntity archived = service.archiveVacancy(10L, 5L);

        assertEquals(VacancyStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void cannotManageSomeoneElsesVacancy() {
        EmployerProfileEntity other = new EmployerProfileEntity();
        other.setId(99L);
        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setId(5L);
        vacancy.setEmployer(other);
        when(vacancyRepository.findWithEmployerById(5L)).thenReturn(Optional.of(vacancy));

        assertThrows(AccessDeniedException.class, () -> service.archiveVacancy(10L, 5L));
    }

    @Test
    void updateVacancyWritesSalaryAndStack() {
        VacancyEntity vacancy = ownedVacancy(7L);
        when(vacancyRepository.findWithEmployerById(7L)).thenReturn(Optional.of(vacancy));
        when(vacancyRepository.save(any(VacancyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateVacancy(10L, 7L, 250_000, 320_000, "Kotlin, Spring");

        ArgumentCaptor<VacancyEntity> captor = ArgumentCaptor.forClass(VacancyEntity.class);
        verify(vacancyRepository).save(captor.capture());
        assertEquals(250_000, captor.getValue().getSalaryMin());
        assertEquals(320_000, captor.getValue().getSalaryMax());
        assertEquals("Kotlin, Spring", captor.getValue().getRequirementsStack());
    }

    private VacancyEntity ownedVacancy(Long id) {
        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setId(id);
        vacancy.setEmployer(employer);
        vacancy.setStatus(VacancyStatus.ACTIVE);
        vacancy.setSalaryMin(100_000);
        vacancy.setSalaryMax(150_000);
        return vacancy;
    }
}
