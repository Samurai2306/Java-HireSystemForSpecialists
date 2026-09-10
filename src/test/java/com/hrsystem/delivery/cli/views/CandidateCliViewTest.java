package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.ApplicationStatus;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.dto.request.VacancyFilterDto;
import com.hrsystem.dto.response.ApplicationDto;
import com.hrsystem.dto.response.VacancyDetailsDto;
import com.hrsystem.dto.response.VacancySummaryDto;
import com.hrsystem.service.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateCliViewTest {

    @Mock
    private CandidateService candidateService;

    private CliSessionContext sessionContext;
    private ByteArrayOutputStream outContent;
    private UserEntity user;
    private CandidateProfileEntity profile;

    @BeforeEach
    void setUp() {
        sessionContext = new CliSessionContext();
        outContent = new ByteArrayOutputStream();

        user = new UserEntity("damir@test.com", "hash", UserRole.CANDIDATE);
        user.setId(1L);
        profile = new CandidateProfileEntity(user, "Дамир Галимов", "Java Dev", "Java 17, Spring", "123", "@damir");
        profile.setId(10L);
        sessionContext.setCandidateSession(user, profile);
    }

    @Test
    @DisplayName("Просмотр каталога вакансий и выход назад по команде B")
    void showVacancyCatalog_shouldDisplayTableAndExitOnB() {
        String simulatedInput = "B\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        VacancySummaryDto v = new VacancySummaryDto(1L, "Senior Java Dev", "Яндекс", "250 000 RUB", VacancySource.WEBSITE, "10.09.2026");
        when(candidateService.searchVacancies(any(VacancyFilterDto.class)))
                .thenReturn(new PageImpl<>(List.of(v), PageRequest.of(0, 10), 1));

        CandidateCliView view = new CandidateCliView(candidateService, sessionContext, inputValidator, new PrintStream(outContent));
        view.showVacancyCatalog();

        assertThat(outContent.toString()).contains("КАТАЛОГ ВАКАНСИЙ");
        assertThat(outContent.toString()).contains("Senior Java Dev");
        assertThat(outContent.toString()).contains("Яндекс");
        assertThat(outContent.toString()).contains("250 000 RUB");
    }

    @Test
    @DisplayName("Экран 'Мои отклики' отображает поданные заявки со статусами")
    void showMyApplications_shouldRenderTable() {
        String simulatedInput = "B\n";
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        ApplicationDto app = new ApplicationDto();
        app.setId(101L);
        app.setVacancyTitle("Java Developer");
        app.setCompanyName("Тинькофф");
        app.setAppliedAtFormatted("10.09.2026 12:00");
        app.setStatus(ApplicationStatus.APPLIED);

        when(candidateService.getMyApplications(1L)).thenReturn(List.of(app));

        CandidateCliView view = new CandidateCliView(candidateService, sessionContext, inputValidator, new PrintStream(outContent));
        view.showMyApplications();

        assertThat(outContent.toString()).contains("МОИ ОТКЛИКИ И ТРЕКЕР ЗАЯВОК");
        assertThat(outContent.toString()).contains("101");
        assertThat(outContent.toString()).contains("Java Developer");
        assertThat(outContent.toString()).contains("Тинькофф");
    }

    @Test
    @DisplayName("Экран профиля соискателя отображает личные данные")
    void showCandidateProfile_displaysProfile() {
        String simulatedInput = "n\n"; // Do not edit
        InputValidator inputValidator = new InputValidator(
                new ByteArrayInputStream(simulatedInput.getBytes()),
                new PrintStream(outContent)
        );

        when(candidateService.getProfile(1L)).thenReturn(profile);

        CandidateCliView view = new CandidateCliView(candidateService, sessionContext, inputValidator, new PrintStream(outContent));
        view.showCandidateProfile();

        assertThat(outContent.toString()).contains("ПРОФИЛЬ СОИСКАТЕЛЯ");
        assertThat(outContent.toString()).contains("Дамир Галимов");
        assertThat(outContent.toString()).contains("Java Dev");
        assertThat(outContent.toString()).contains("@damir");
    }
}
