package com.hrsystem.delivery.cli;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class CliSessionContext {

    private UserEntity currentUser;
    private CandidateProfileEntity candidateProfile;
    private EmployerProfileEntity employerProfile;

    public void login(UserEntity user, CandidateProfileEntity candidateProfile, EmployerProfileEntity employerProfile) {
        this.currentUser = user;
        this.candidateProfile = candidateProfile;
        this.employerProfile = employerProfile;
    }

    public void logout() {
        this.currentUser = null;
        this.candidateProfile = null;
        this.employerProfile = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public UserEntity getCurrentUser() {
        return currentUser;
    }

    public UserRole getRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public CandidateProfileEntity getCandidateProfile() {
        return candidateProfile;
    }

    public EmployerProfileEntity getEmployerProfile() {
        return employerProfile;
    }

    public Long requireCandidateId() {
        if (candidateProfile == null) {
            throw new IllegalStateException("Сессия соискателя не инициализирована");
        }
        return candidateProfile.getId();
    }

    public Long requireEmployerId() {
        if (employerProfile == null) {
            throw new IllegalStateException("Сессия работодателя не инициализирована");
        }
        return employerProfile.getId();
    }
}
