package com.hrsystem.service;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;

public interface AuthService {

    UserEntity registerCandidate(UserRegistrationDto dto);

    UserEntity registerEmployer(String email, String rawPassword, String companyName, String contactPerson, String websiteUrl);

    UserEntity register(UserRegistrationDto dto);

    UserEntity login(UserLoginDto dto);

    UserEntity login(String email, String rawPassword);

    CandidateProfileEntity getCandidateProfile(UserEntity user);

    EmployerProfileEntity getEmployerProfile(UserEntity user);

    String hashPassword(String rawPassword);

    boolean verifyPassword(String rawPassword, String passwordHash);
}
