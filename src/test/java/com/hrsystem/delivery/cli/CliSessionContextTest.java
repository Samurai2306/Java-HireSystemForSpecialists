package com.hrsystem.delivery.cli;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CliSessionContextTest {

    private CliSessionContext context;

    @BeforeEach
    void setUp() {
        context = new CliSessionContext();
    }

    @Test
    @DisplayName("Начальное состояние сессии - не авторизован (Гость)")
    void initialState_shouldBeGuest() {
        assertThat(context.isAuthorized()).isFalse();
        assertThat(context.isGuest()).isTrue();
        assertThat(context.getCurrentRole()).isEqualTo(UserRole.GUEST);
        assertThat(context.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("Установка сессии соискателя")
    void setCandidateSession_shouldReflectCandidateRole() {
        UserEntity user = new UserEntity("damir@test.com", "hash", UserRole.CANDIDATE);
        user.setId(5L);
        CandidateProfileEntity profile = new CandidateProfileEntity(user, "Дамир", "Java Dev", "Java", "123", "@tg");

        context.setCandidateSession(user, profile);

        assertThat(context.isAuthorized()).isTrue();
        assertThat(context.isCandidate()).isTrue();
        assertThat(context.isEmployer()).isFalse();
        assertThat(context.isAdmin()).isFalse();
        assertThat(context.getCurrentUserId()).isEqualTo(5L);
        assertThat(context.getCurrentCandidateProfile().getFullName()).isEqualTo("Дамир");

        // Logout
        context.logout();
        assertThat(context.isAuthorized()).isFalse();
        assertThat(context.getCurrentUser()).isNull();
        assertThat(context.getCurrentCandidateProfile()).isNull();
    }

    @Test
    @DisplayName("Установка сессии работодателя")
    void setEmployerSession_shouldReflectEmployerRole() {
        UserEntity user = new UserEntity("hr@company.com", "hash", UserRole.EMPLOYER);
        user.setId(7L);
        EmployerProfileEntity profile = new EmployerProfileEntity(user, "ООО Ромашка", "HR", "https://site.com");

        context.setEmployerSession(user, profile);

        assertThat(context.isAuthorized()).isTrue();
        assertThat(context.isEmployer()).isTrue();
        assertThat(context.isCandidate()).isFalse();
        assertThat(context.getCurrentEmployerProfile().getCompanyName()).isEqualTo("ООО Ромашка");
    }
}
