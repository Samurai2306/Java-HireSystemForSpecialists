package com.hrsystem;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.request.ApplyVacancyDto;
import com.hrsystem.dto.request.CandidateProfileUpdateDto;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.exception.DuplicateApplicationException;
import com.hrsystem.service.AuthService;
import com.hrsystem.service.CandidateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(args = "--no-cli")
@ActiveProfiles("test")
@Transactional
class CandidateCliFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CandidateService candidateService;

    @Test
    @DisplayName("Сквозной сценарий защиты (DoD 1-6): Регистрация -> Вход -> Поиск с фильтром -> Отклик -> Блокировка дубля -> Отзыв заявки")
    void fullCandidateScenario() {
        // 1. DoD-1: Регистрация соискателя
        UserRegistrationDto regDto = new UserRegistrationDto(
                "candidate.demo@test.com",
                "Password123!",
                "Дамир Студент",
                "Middle Java Developer",
                "Java 17, Spring Boot, SQL, Git",
                "+7 (999) 000-11-22",
                "@damir_student",
                "https://github.com/damir-student"
        );
        UserEntity user = authService.registerCandidate(regDto);
        assertThat(user).isNotNull();
        assertThat(user.getRole()).isEqualTo(UserRole.CANDIDATE);
        assertThat(user.getPasswordHash()).startsWith("$2a$10$");

        // 2. DoD-2: Проверка аутентификации
        // Успешный вход
        UserEntity loggedIn = authService.login(new UserLoginDto("candidate.demo@test.com", "Password123!"));
        assertThat(loggedIn.getId()).isEqualTo(user.getId());

        // Вход с неверным паролем
        assertThatThrownBy(() -> authService.login(new UserLoginDto("candidate.demo@test.com", "WrongPassword")))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Неверный пароль");

        // 3. DoD-3 & DoD-4: Каталог вакансий, пагинация по 10 и фильтрация
        VacancyFilterDto filter = new VacancyFilterDto("Java", 180000, VacancySource.ALL, 1, 10);
        Page<VacancySummaryDto> searchResult = candidateService.searchVacancies(filter);

        assertThat(searchResult.getContent()).isNotEmpty();
        assertThat(searchResult.getSize()).isEqualTo(10);
        for (VacancySummaryDto v : searchResult.getContent()) {
            assertThat(v.getTitle().toLowerCase()).contains("java");
        }

        Long targetVacancyId = searchResult.getContent().get(0).getId();

        // 4. Подача первого отклика
        ApplyVacancyDto applyDto = new ApplyVacancyDto(targetVacancyId, "Здравствуйте! Прошу рассмотреть мое резюме.", false);
        ApplicationDto application = candidateService.applyForVacancy(user.getId(), applyDto);

        assertThat(application).isNotNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getVacancyId()).isEqualTo(targetVacancyId);

        // 5. DoD-5: Повторный отклик на ту же вакансию блокируется
        assertThatThrownBy(() -> candidateService.applyForVacancy(user.getId(), applyDto))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Повторный отклик запрещен");

        // 6. Экран «Мои отклики»
        List<ApplicationDto> myApps = candidateService.getMyApplications(user.getId());
        assertThat(myApps).hasSize(1);
        assertThat(myApps.get(0).getId()).isEqualTo(application.getId());
        assertThat(myApps.get(0).getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        // 7. DoD-6: Отзыв заявки переводит в WITHDRAWN
        ApplicationDto withdrawn = candidateService.withdrawApplication(user.getId(), application.getId());
        assertThat(withdrawn.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);

        // 8. Просмотр и обновление профиля
        CandidateProfileEntity profile = candidateService.getProfile(user.getId());
        assertThat(profile.getFullName()).isEqualTo("Дамир Студент");

        CandidateProfileUpdateDto updateDto = new CandidateProfileUpdateDto(
                "Дамир Студент (Обновленный)",
                "Senior Java Engineer",
                "Java 17, Spring Boot 3, Microservices, Kafka",
                "+7 (999) 777-88-99",
                "@damir_senior",
                "https://github.com/damir-senior"
        );
        CandidateProfileEntity updated = candidateService.updateProfile(user.getId(), updateDto);
        assertThat(updated.getFullName()).isEqualTo("Дамир Студент (Обновленный)");
        assertThat(updated.getTargetTitle()).isEqualTo("Senior Java Engineer");
    }
}
