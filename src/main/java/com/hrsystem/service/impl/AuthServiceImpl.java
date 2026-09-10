package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           CandidateProfileRepository candidateProfileRepository,
                           EmployerProfileRepository employerProfileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserEntity register(UserRegistrationDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new AuthenticationException("Пользователь с email " + dto.getEmail() + " уже зарегистрирован");
        }
        if (dto.getRole() != UserRole.CANDIDATE && dto.getRole() != UserRole.EMPLOYER) {
            throw new AuthenticationException("Регистрация доступна только для соискателя или работодателя");
        }
        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setActive(true);
        user = userRepository.save(user);

        if (dto.getRole() == UserRole.CANDIDATE) {
            CandidateProfileEntity profile = new CandidateProfileEntity();
            profile.setUser(user);
            profile.setFullName(dto.getFullName());
            profile.setTargetTitle(dto.getTargetTitle());
            profile.setSkills(dto.getSkills());
            profile.setPhone(dto.getPhone());
            profile.setTelegram(dto.getTelegram());
            candidateProfileRepository.save(profile);
        } else {
            EmployerProfileEntity profile = new EmployerProfileEntity();
            profile.setUser(user);
            profile.setCompanyName(dto.getCompanyName());
            profile.setContactPerson(dto.getContactPerson());
            profile.setWebsiteUrl(dto.getWebsiteUrl());
            profile.setDescription(dto.getCompanyDescription());
            employerProfileRepository.save(profile);
        }
        return user;
    }

    @Override
    public UserEntity login(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new AuthenticationException("Неверный email или пароль"));
        if (!user.isActive()) {
            throw new AuthenticationException("Учётная запись заблокирована");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Неверный email или пароль");
        }
        return user;
    }
}
