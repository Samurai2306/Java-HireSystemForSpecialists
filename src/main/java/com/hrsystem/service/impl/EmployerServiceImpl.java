package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.request.CreateVacancyDto;
import com.hrsystem.dto.response.EmployerVacancyRowDto;
import com.hrsystem.exception.AccessDeniedException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.EmployerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployerServiceImpl implements EmployerService {

    private final EmployerProfileRepository employerProfileRepository;
    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;

    public EmployerServiceImpl(EmployerProfileRepository employerProfileRepository,
                               VacancyRepository vacancyRepository,
                               ApplicationRepository applicationRepository) {
        this.employerProfileRepository = employerProfileRepository;
        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public VacancyEntity createVacancy(Long employerProfileId, CreateVacancyDto dto) {
        EmployerProfileEntity employer = requireEmployer(employerProfileId);
        validateSalary(dto.getSalaryMin(), dto.getSalaryMax());

        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setEmployer(employer);
        vacancy.setTitle(dto.getTitle().trim());
        vacancy.setCompanyName(employer.getCompanyName());
        vacancy.setSalaryMin(dto.getSalaryMin());
        vacancy.setSalaryMax(dto.getSalaryMax());
        vacancy.setCurrency(dto.getCurrency() == null ? Currency.RUB : dto.getCurrency());
        vacancy.setDescription(dto.getDescription().trim());
        vacancy.setRequirementsStack(blankToNull(dto.getRequirementsStack()));
        vacancy.setEmploymentType(dto.getEmploymentType() == null ? EmploymentType.REMOTE : dto.getEmploymentType());
        vacancy.setLocation(dto.getLocation() == null || dto.getLocation().isBlank() ? "Не указано" : dto.getLocation().trim());
        vacancy.setSourceType(VacancySource.MANUAL);
        vacancy.setParsed(false);
        vacancy.setStatus(VacancyStatus.ACTIVE);
        return vacancyRepository.save(vacancy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployerVacancyRowDto> listMyVacancies(Long employerProfileId) {
        requireEmployer(employerProfileId);
        return vacancyRepository.findByEmployerIdOrderByPublishedAtDesc(employerProfileId).stream()
                .map(v -> EmployerVacancyRowDto.from(v, applicationRepository.countByVacancyId(v.getId())))
                .toList();
    }

    @Override
    public VacancyEntity updateVacancy(Long employerProfileId, Long vacancyId, Integer salaryMin,
                                       Integer salaryMax, String requirements) {
        VacancyEntity vacancy = getOwnedVacancy(employerProfileId, vacancyId);
        if (salaryMin != null || salaryMax != null) {
            Integer min = salaryMin != null ? salaryMin : vacancy.getSalaryMin();
            Integer max = salaryMax != null ? salaryMax : vacancy.getSalaryMax();
            validateSalary(min, max);
            vacancy.setSalaryMin(min);
            vacancy.setSalaryMax(max);
        }
        if (requirements != null) {
            vacancy.setRequirementsStack(requirements.isBlank() ? null : requirements.trim());
        }
        return vacancyRepository.save(vacancy);
    }

    @Override
    public VacancyEntity archiveVacancy(Long employerProfileId, Long vacancyId) {
        VacancyEntity vacancy = getOwnedVacancy(employerProfileId, vacancyId);
        vacancy.setStatus(VacancyStatus.ARCHIVED);
        return vacancyRepository.save(vacancy);
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyEntity getOwnedVacancy(Long employerProfileId, Long vacancyId) {
        VacancyEntity vacancy = vacancyRepository.findWithEmployerById(vacancyId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия #" + vacancyId + " не найдена"));
        if (vacancy.getEmployer() == null || !employerProfileId.equals(vacancy.getEmployer().getId())) {
            throw new AccessDeniedException("Нельзя управлять чужой вакансией #" + vacancyId);
        }
        return vacancy;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerProfileEntity getProfile(Long employerProfileId) {
        return requireEmployer(employerProfileId);
    }

    @Override
    public EmployerProfileEntity updateProfile(Long employerProfileId, String companyName, String contactPerson,
                                               String websiteUrl, String description) {
        EmployerProfileEntity profile = requireEmployer(employerProfileId);
        if (companyName != null && !companyName.isBlank()) {
            profile.setCompanyName(companyName.trim());
        }
        if (contactPerson != null) {
            profile.setContactPerson(contactPerson.isBlank() ? null : contactPerson.trim());
        }
        if (websiteUrl != null) {
            profile.setWebsiteUrl(websiteUrl.isBlank() ? null : websiteUrl.trim());
        }
        if (description != null) {
            profile.setDescription(description.isBlank() ? null : description.trim());
        }
        return employerProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationEntity> getApplicationsForVacancy(Long employerProfileId, Long vacancyId) {
        getOwnedVacancy(employerProfileId, vacancyId);
        return applicationRepository.findByVacancyIdOrderByCreatedAtDesc(vacancyId);
    }

    private EmployerProfileEntity requireEmployer(Long employerProfileId) {
        return employerProfileRepository.findById(employerProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль работодателя #" + employerProfileId + " не найден"));
    }

    private void validateSalary(Integer min, Integer max) {
        if (min != null && min < 0) {
            throw new IllegalArgumentException("Минимальная зарплата не может быть отрицательной");
        }
        if (min != null && max != null && max < min) {
            throw new IllegalArgumentException("Максимальная зарплата не может быть меньше минимальной");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
