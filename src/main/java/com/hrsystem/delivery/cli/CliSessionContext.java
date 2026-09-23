package com.hrsystem.delivery.cli;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import org.springframework.stereotype.Component;

/** Единое состояние авторизованного пользователя для всего CLI. */
@Component
public class CliSessionContext {

    private UserEntity currentUser;
    private CandidateProfileEntity currentCandidateProfile;
    private EmployerProfileEntity currentEmployerProfile;

    public synchronized void setCandidateSession(UserEntity user, CandidateProfileEntity candidateProfile) {
        this.currentUser = user;
        this.currentCandidateProfile = candidateProfile;
        this.currentEmployerProfile = null;
    }

    public synchronized void setEmployerSession(UserEntity user, EmployerProfileEntity employerProfile) {
        this.currentUser = user;
        this.currentCandidateProfile = null;
        this.currentEmployerProfile = employerProfile;
    }

    public synchronized void setAdminSession(UserEntity user) {
        this.currentUser = user;
        this.currentCandidateProfile = null;
        this.currentEmployerProfile = null;
    }

    public synchronized void updateCandidateProfile(CandidateProfileEntity profile) {
        this.currentCandidateProfile = profile;
    }

    public synchronized void logout() {
        this.currentUser = null;
        this.currentCandidateProfile = null;
        this.currentEmployerProfile = null;
    }

    public synchronized boolean isAuthorized() {
        return currentUser != null;
    }

    public synchronized boolean isGuest() {
        return currentUser == null;
    }

    public synchronized boolean isCandidate() {
        return isAuthorized() && currentUser.getRole() == UserRole.CANDIDATE;
    }

    public synchronized boolean isEmployer() {
        return isAuthorized() && currentUser.getRole() == UserRole.EMPLOYER;
    }

    public synchronized boolean isAdmin() {
        return isAuthorized() && currentUser.getRole() == UserRole.ADMIN;
    }

    /** GUEST, если пользователь не авторизован — используется для полиморфного выбора меню. */
    public synchronized UserRole getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : UserRole.GUEST;
    }

    public synchronized UserEntity getCurrentUser() {
        return currentUser;
    }

    public synchronized Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public synchronized CandidateProfileEntity getCurrentCandidateProfile() {
        return currentCandidateProfile;
    }

    public synchronized EmployerProfileEntity getCurrentEmployerProfile() {
        return currentEmployerProfile;
    }

    public synchronized Long requireEmployerId() {
        if (currentEmployerProfile == null) {
            throw new IllegalStateException("Сессия работодателя не инициализирована");
        }
        return currentEmployerProfile.getId();
    }
}
