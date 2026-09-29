package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.CandidateProfileEntity;
import com.hrsystem.domain.entity.EmployerProfileEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.dto.request.UserLoginDto;
import com.hrsystem.dto.request.UserRegistrationDto;
import com.hrsystem.exception.AuthenticationException;
import com.hrsystem.exception.ValidationException;
import com.hrsystem.repository.CandidateProfileRepository;
import com.hrsystem.repository.EmployerProfileRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           CandidateProfileRepository candidateProfileRepository,
                           EmployerProfileRepository employerProfileRepository) {
        this(userRepository, candidateProfileRepository, employerProfileRepository,
                new BCryptPasswordEncoder(10));
    }

    @Autowired
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
    @Transactional
    public UserEntity registerCandidate(UserRegistrationDto dto) {
        validateEmail(dto.getEmail());
        if (dto.getPassword() == null || dto.getPassword().trim().length() < 4) {
            throw new ValidationException("Пароль должен содержать не менее 4 символов.");
        }
        if (dto.getFullName() == null || dto.getFullName().trim().isEmpty()) {
            throw new ValidationException("ФИО соискателя обязательно для заполнения.");
        }

        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Пользователь с таким email уже зарегистрирован.");
        }

        String passwordHash = hashPassword(dto.getPassword().trim());
        UserEntity user = new UserEntity(normalizedEmail, passwordHash, UserRole.CANDIDATE);
        UserEntity savedUser = userRepository.save(user);

        CandidateProfileEntity profile = new CandidateProfileEntity(
                savedUser,
                dto.getFullName().trim(),
                dto.getTargetTitle() != null ? dto.getTargetTitle().trim() : null,
                dto.getSkills() != null ? dto.getSkills().trim() : null,
                dto.getPhone() != null ? dto.getPhone().trim() : null,
                dto.getTelegram() != null ? dto.getTelegram().trim() : null
        );
        profile.setPortfolioLinks(dto.getPortfolioLinks() != null ? dto.getPortfolioLinks().trim() : null);
        candidateProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    @Transactional
    public UserEntity registerEmployer(String email, String rawPassword, String companyName, String contactPerson, String websiteUrl) {
        validateEmail(email);
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new ValidationException("Пароль должен содержать не менее 4 символов.");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new ValidationException("Название компании обязательно для заполнения.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Пользователь с таким email уже зарегистрирован.");
        }

        String passwordHash = hashPassword(rawPassword.trim());
        UserEntity user = new UserEntity(normalizedEmail, passwordHash, UserRole.EMPLOYER);
        UserEntity savedUser = userRepository.save(user);

        EmployerProfileEntity profile = new EmployerProfileEntity(
                savedUser,
                companyName.trim(),
                contactPerson != null ? contactPerson.trim() : null,
                websiteUrl != null ? websiteUrl.trim() : null
        );
        employerProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    @Transactional
    public UserEntity register(UserRegistrationDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new AuthenticationException("Email обязателен для регистрации");
        }
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail) || userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AuthenticationException("Пользователь с email " + dto.getEmail() + " уже зарегистрирован");
        }
        if (dto.getRole() == null || dto.getRole() == UserRole.CANDIDATE) {
            return registerCandidate(dto);
        } else if (dto.getRole() == UserRole.EMPLOYER) {
            UserEntity user = registerEmployer(dto.getEmail(), dto.getPassword(), dto.getCompanyName(),
                    dto.getContactPerson(), dto.getWebsiteUrl());
            if (dto.getCompanyDescription() != null && !dto.getCompanyDescription().isBlank()) {
                EmployerProfileEntity profile = employerProfileRepository.findByUserId(user.getId()).orElse(null);
                if (profile != null) {
                    profile.setDescription(dto.getCompanyDescription().trim());
                    employerProfileRepository.save(profile);
                }
            }
            return user;
        } else {
            throw new AuthenticationException("Регистрация доступна только для соискателя или работодателя");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity login(UserLoginDto dto) {
        if (dto == null) {
            throw new AuthenticationException("Данные для входа не предоставлены.");
        }
        return login(dto.getEmail(), dto.getPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity login(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty() || rawPassword == null) {
            throw new AuthenticationException("Email и пароль обязательны для входа.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByEmail(email.trim()))
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .orElseThrow(() -> new AuthenticationException("Пользователь с таким email не найден."));

        if (!verifyPassword(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Неверный пароль.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Учетная запись заблокирована администратором.");
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileEntity getCandidateProfile(UserEntity user) {
        if (user == null) return null;
        return candidateProfileRepository.findByUserId(user.getId())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerProfileEntity getEmployerProfile(UserEntity user) {
        if (user == null) return null;
        return employerProfileRepository.findByUserId(user.getId())
                .orElse(null);
    }

    @Override
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean verifyPassword(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email обязателен для заполнения.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Некорректный формат email (пример: user@example.com).");
        }
    }
}
