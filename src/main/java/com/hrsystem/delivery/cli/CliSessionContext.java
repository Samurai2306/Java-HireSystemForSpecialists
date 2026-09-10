package com.hrsystem.delivery.cli;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class CliSessionContext {

    private UserEntity currentUser;
    private CandidateProfileEntity currentCandidateProfile;
    private EmployerProfileEntity currentEmployerProfile;

    public CliSessionContext() {
    }

    public synchronized void login(UserEntity user, CandidateProfileEntity candidateProfile, EmployerProfileEntity employerProfile) {
        this.currentUser = user;
        this.currentCandidateProfile = candidateProfile;
        this.currentEmployerProfile = employerProfile;
    }

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

    public synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public synchronized boolean isGuest() {
        return currentUser == null;
    }

    public synchronized boolean isCandidate() {
        return isAuthorized() && UserRole.CANDIDATE.equals(currentUser.getRole());
    }

    public synchronized boolean isEmployer() {
        return isAuthorized() && UserRole.EMPLOYER.equals(currentUser.getRole());
    }

    public synchronized boolean isAdmin() {
        return isAuthorized() && UserRole.ADMIN.equals(currentUser.getRole());
    }

    public synchronized UserRole getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : UserRole.GUEST;
    }

    public synchronized UserRole getRole() {
        return currentUser == null ? null : currentUser.getRole();
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

    public synchronized CandidateProfileEntity getCandidateProfile() {
        return currentCandidateProfile;
    }

    public synchronized EmployerProfileEntity getCurrentEmployerProfile() {
        return currentEmployerProfile;
    }

    public synchronized EmployerProfileEntity getEmployerProfile() {
        return currentEmployerProfile;
    }

    public synchronized Long requireCandidateId() {
        if (currentCandidateProfile == null) {
            throw new IllegalStateException("Сессия соискателя не инициализирована");
        }
        return currentCandidateProfile.getId();
    }

    public synchronized Long requireEmployerId() {
        if (currentEmployerProfile == null) {
            throw new IllegalStateException("Сессия работодателя не инициализирована");
        }
        return currentEmployerProfile.getId();
    }
}
