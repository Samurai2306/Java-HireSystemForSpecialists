package com.hrsystem.service;

import com.hrsystem.domain.entity.ApplicationEntity;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.repository.ApplicationRepository;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.repository.VacancyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(SeedDataGenerator.class);

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final VacancyRepository vacancyRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataGenerator(UserRepository userRepository,
                             CandidateProfileRepository candidateProfileRepository,
                             EmployerProfileRepository employerProfileRepository,
                             VacancyRepository vacancyRepository,
                             ApplicationRepository applicationRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.vacancyRepository = vacancyRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void seed() {
        if (userRepository.existsByEmailIgnoreCase("admin@hrsystem.local")) {
            log.info("Seed data already present, skip");
            return;
        }

        UserEntity admin = user("admin@hrsystem.local", "admin123", UserRole.ADMIN);
        UserEntity employerUser = user("employer@hrsystem.local", "employer123", UserRole.EMPLOYER);
        UserEntity candidateUser = user("candidate@hrsystem.local", "candidate123", UserRole.CANDIDATE);

        EmployerProfileEntity employer = new EmployerProfileEntity();
        employer.setUser(employerUser);
        employer.setCompanyName("TechNova");
        employer.setContactPerson("Анна Смирнова");
        employer.setWebsiteUrl("https://technova.example");
        employer.setDescription("Продуктовая IT-компания: backend, frontend, data.");
        employer = employerProfileRepository.save(employer);

        CandidateProfileEntity candidate = new CandidateProfileEntity();
        candidate.setUser(candidateUser);
        candidate.setFullName("Иван Петров");
        candidate.setTargetTitle("Java Backend Developer");
        candidate.setSkills("Java, Spring Boot, PostgreSQL, Kafka");
        candidate.setPhone("+7-900-000-00-01");
        candidate.setTelegram("@ivan_dev");
        candidate.setPortfolioLinks("https://github.com/ivan-dev");
        candidate = candidateProfileRepository.save(candidate);

        VacancyEntity javaJob = vacancy(
                employer,
                "Java Backend Developer",
                "TechNova",
                220_000,
                300_000,
                "Java, Spring Boot, PostgreSQL",
                "Разработка сервисов найма. Коммерческий опыт от 3 лет.",
                VacancyStatus.ACTIVE
        );
        vacancy(
                employer,
                "Frontend React Developer",
                "TechNova",
                180_000,
                240_000,
                "React, TypeScript, Redux",
                "Интерфейс кабинета соискателя и работодателя.",
                VacancyStatus.ARCHIVED
        );
        vacancy(
                employer,
                "QA Automation Engineer",
                "TechNova",
                160_000,
                210_000,
                "Java, JUnit, REST Assured",
                "Автотесты API и регрессионный контур.",
                VacancyStatus.ACTIVE
        );

        ApplicationEntity application = new ApplicationEntity();
        application.setVacancy(javaJob);
        application.setCandidate(candidate);
        application.setCoverLetter("Здравствуйте! 3 года коммерческой разработки на Spring Boot и PostgreSQL.");
        application.setStatus(ApplicationStatus.APPLIED);
        applicationRepository.save(application);

        log.info("Seed users: admin@hrsystem.local / admin123, employer@hrsystem.local / employer123, candidate@hrsystem.local / candidate123");
        log.info("Admin id={}, employer={}, candidate={}", admin.getId(), employerUser.getId(), candidateUser.getId());
    }

    private UserEntity user(String email, String password, UserRole role) {
        UserEntity entity = new UserEntity();
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setRole(role);
        entity.setActive(true);
        return userRepository.save(entity);
    }

    private VacancyEntity vacancy(EmployerProfileEntity employer, String title, String company,
                                  int min, int max, String stack, String description, VacancyStatus status) {
        VacancyEntity vacancy = new VacancyEntity();
        vacancy.setEmployer(employer);
        vacancy.setTitle(title);
        vacancy.setCompanyName(company);
        vacancy.setSalaryMin(min);
        vacancy.setSalaryMax(max);
        vacancy.setCurrency(Currency.RUB);
        vacancy.setRequirementsStack(stack);
        vacancy.setDescription(description);
        vacancy.setEmploymentType(EmploymentType.REMOTE);
        vacancy.setLocation("Москва / Remote");
        vacancy.setSourceType(VacancySource.MANUAL);
        vacancy.setParsed(false);
        vacancy.setStatus(status);
        return vacancyRepository.save(vacancy);
    }
}
