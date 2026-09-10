package com.hrsystem.service;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.*;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.ParsingSourceRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.repository.VacancyRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class SeedDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final VacancyRepository vacancyRepository;
    private final ParsingSourceRepository parsingSourceRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(UserRepository userRepository,
                               CandidateProfileRepository candidateProfileRepository,
                               EmployerProfileRepository employerProfileRepository,
                               VacancyRepository vacancyRepository,
                               ParsingSourceRepository parsingSourceRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.vacancyRepository = vacancyRepository;
        this.parsingSourceRepository = parsingSourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            seedDemoUsers();
        }
        if (vacancyRepository.count() == 0) {
            seedDemoVacancies();
        }
        if (parsingSourceRepository.count() == 0) {
            seedDemoSources();
        }
    }

    private void seedDemoUsers() {
        // Damir's demo accounts
        String adminHash = passwordEncoder.encode("admin123");
        UserEntity admin = new UserEntity("admin@hrsystem.com", adminHash, UserRole.ADMIN);
        userRepository.save(admin);

        String candidateHash = passwordEncoder.encode("candidate123");
        UserEntity candidateUser = new UserEntity("damir@candidate.com", candidateHash, UserRole.CANDIDATE);
        userRepository.save(candidateUser);

        CandidateProfileEntity candidateProfile = new CandidateProfileEntity(
                candidateUser,
                "Дамир Галимов",
                "Senior Java Developer",
                "Java 17, Spring Boot, PostgreSQL, Docker, Kafka, Git",
                "+7 (999) 111-22-33",
                "@damir_dev"
        );
        candidateProfile.setPortfolioLinks("https://github.com/damir-galimov");
        candidateProfileRepository.save(candidateProfile);

        String employerHash = passwordEncoder.encode("employer123");
        UserEntity employerUser = new UserEntity("hr@yandex-team.ru", employerHash, UserRole.EMPLOYER);
        userRepository.save(employerUser);

        EmployerProfileEntity employerProfile = new EmployerProfileEntity(
                employerUser,
                "Яндекс",
                "Анна Смирнова",
                "https://yandex.ru/jobs"
        );
        employerProfile.setDescription("Ведущая IT-компания России.");
        employerProfileRepository.save(employerProfile);

        // Gleb's demo accounts (.local)
        UserEntity glebAdmin = new UserEntity("admin@hrsystem.local", passwordEncoder.encode("admin123"), UserRole.ADMIN);
        userRepository.save(glebAdmin);

        UserEntity glebEmployer = new UserEntity("employer@hrsystem.local", passwordEncoder.encode("employer123"), UserRole.EMPLOYER);
        userRepository.save(glebEmployer);

        EmployerProfileEntity technova = new EmployerProfileEntity(
                glebEmployer,
                "TechNova",
                "Анна Смирнова",
                "https://technova.example"
        );
        technova.setDescription("Продуктовая IT-компания: backend, frontend, data.");
        employerProfileRepository.save(technova);

        UserEntity glebCandidate = new UserEntity("candidate@hrsystem.local", passwordEncoder.encode("candidate123"), UserRole.CANDIDATE);
        userRepository.save(glebCandidate);

        CandidateProfileEntity ivanProfile = new CandidateProfileEntity(
                glebCandidate,
                "Иван Петров",
                "Java Backend Developer",
                "Java, Spring Boot, PostgreSQL, Kafka",
                "+7-900-000-00-01",
                "@ivan_dev"
        );
        ivanProfile.setPortfolioLinks("https://github.com/ivan-dev");
        candidateProfileRepository.save(ivanProfile);
    }

    private void seedDemoVacancies() {
        EmployerProfileEntity yandex = employerProfileRepository.findAll().stream().findFirst().orElse(null);

        createVacancy("Senior Java Backend Engineer", "Яндекс", 280000, 350000, Currency.RUB,
                "Разработка высоконагруженных сервисов платформы поиска.",
                "Java 17, Spring Boot 3, PostgreSQL, Kafka, Redis, Kubernetes",
                "Москва / Удаленно", EmploymentType.HYBRID, VacancySource.MANUAL, yandex);

        createVacancy("Middle Java Developer", "Тинькофф", 180000, 240000, Currency.RUB,
                "Разработка финансовых сервисов в продуктовой команде эквайринга.",
                "Java 17, Spring Boot, Hibernate, PostgreSQL, Docker",
                "Москва", EmploymentType.REMOTE, VacancySource.WEBSITE, null);

        createVacancy("Lead Java Architect", "Сбер", 350000, 450000, Currency.RUB,
                "Проектирование архитектуры корпоративной платформы нового поколения.",
                "Java 21, Spring Cloud, Kafka, Distributed Systems, Cloud Architecture",
                "Москва", EmploymentType.OFFICE, VacancySource.WEBSITE, null);

        createVacancy("Junior+ Java Developer", "Ozon", 120000, 160000, Currency.RUB,
                "Поддержка микросервисов логистической платформы.",
                "Java 17, Spring Boot, SQL, Git, JUnit",
                "Санкт-Петербург", EmploymentType.HYBRID, VacancySource.WEBSITE, null);

        createVacancy("Java Software Engineer (Platform)", "VK", 220000, 290000, Currency.RUB,
                "Разработка ядра высоконагруженных коммуникационных сервисов.",
                "Java 17, Spring, PostgreSQL, Netty, HighLoad",
                "Санкт-Петербург", EmploymentType.REMOTE, VacancySource.TELEGRAM, null);

        createVacancy("Golang Developer", "Авито", 240000, 320000, Currency.RUB,
                "Разработка микросервисов поисковой выдачи и модерации.",
                "Go, Docker, Kubernetes, PostgreSQL, gRPC",
                "Москва", EmploymentType.REMOTE, VacancySource.WEBSITE, null);

        createVacancy("Senior Python / ML Engineer", "Kaspersky", 260000, 330000, Currency.RUB,
                "Создание алгоритмов поведенческого анализа и ML-моделей.",
                "Python, PyTorch, SQL, FastApi, Docker",
                "Москва", EmploymentType.OFFICE, VacancySource.WEBSITE, null);

        createVacancy("Frontend Engineer (React / TypeScript)", "Mindbox", 160000, 220000, Currency.RUB,
                "Разработка B2B интерфейсов платформы автоматизации маркетинга.",
                "TypeScript, React, Redux Toolkit, Webpack",
                "Удаленно", EmploymentType.REMOTE, VacancySource.TELEGRAM, null);

        createVacancy("DevOps / SRE Engineer", "Wildberries", 250000, 340000, Currency.RUB,
                "Поддержка инфраструктуры Kubernetes и мониторинга.",
                "Linux, Kubernetes, Ansible, Terraform, Prometheus",
                "Москва", EmploymentType.REMOTE, VacancySource.TELEGRAM, null);

        createVacancy("QA Automation Engineer (Java)", "Альфа-Банк", 150000, 200000, Currency.RUB,
                "Автоматизация регрессионного и нагрузочного тестирования банковских систем.",
                "Java, Selenium, Allure, RestAssured, CI/CD",
                "Москва", EmploymentType.HYBRID, VacancySource.WEBSITE, null);

        createVacancy("Fullstack Java / Angular Developer", "Positive Technologies", 210000, 270000, Currency.RUB,
                "Разработка продуктов в области кибербезопасности.",
                "Java, Spring Boot, Angular, PostgreSQL",
                "Москва", EmploymentType.REMOTE, VacancySource.WEBSITE, null);

        createVacancy("Senior Java Developer (Billing)", "МТС", 270000, 330000, Currency.RUB,
                "Разработка биллинговой системы реального времени.",
                "Java 17, Oracle DB, Spring Boot, Kafka",
                "Москва", EmploymentType.HYBRID, VacancySource.TELEGRAM, null);

        createVacancy("Junior Java Developer (Trainee)", "Lamoda", 80000, 110000, Currency.RUB,
                "Старт карьеры в сильной команде backend-разработки e-commerce.",
                "Java Core, Collections, SQL, Git",
                "Москва", EmploymentType.OFFICE, VacancySource.WEBSITE, null);

        createVacancy("Backend Team Lead (Java)", "HeadHunter", 380000, 480000, Currency.RUB,
                "Управление командой backend-разработки сервиса резюме.",
                "Java, Spring Boot, Management, System Design, PostgreSQL",
                "Москва", EmploymentType.REMOTE, VacancySource.MANUAL, null);

        createVacancy("Site Reliability Engineer (Java Services)", "Selectel", 200000, 260000, Currency.RUB,
                "Обеспечение надежности и отказоустойчивости облачных сервисов.",
                "Java, Linux, Grafana, Ansible, Docker",
                "Санкт-Петербург", EmploymentType.REMOTE, VacancySource.WEBSITE, null);
    }

    private void createVacancy(String title, String company, Integer minSal, Integer maxSal,
                              Currency curr, String desc, String stack, String loc,
                              EmploymentType empType, VacancySource source, EmployerProfileEntity employer) {
        VacancyEntity v = new VacancyEntity(title, company, minSal, maxSal, curr, desc, stack, loc, empType, source);
        v.setEmployer(employer);
        v.setPublishedAt(Instant.now().minus((long) (Math.random() * 72), ChronoUnit.HOURS));
        vacancyRepository.save(v);
    }

    private void seedDemoSources() {
        ParsingSourceEntity hh = new ParsingSourceEntity();
        hh.setName("HeadHunter (hh.ru)");
        hh.setSourceType(VacancySource.WEBSITE);
        hh.setBaseUrl("https://hh.ru/search/vacancy");
        hh.setActive(true);
        parsingSourceRepository.save(hh);

        ParsingSourceEntity tg = new ParsingSourceEntity();
        tg.setName("Telegram Java Jobs");
        tg.setSourceType(VacancySource.TELEGRAM);
        tg.setBaseUrl("https://t.me/s/javajobs");
        tg.setActive(true);
        parsingSourceRepository.save(tg);
    }
}
