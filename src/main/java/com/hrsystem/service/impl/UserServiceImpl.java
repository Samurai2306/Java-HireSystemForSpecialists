package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.dto.response.UserResponseDto;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserResponse(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден."));
        return new UserResponseDto(user.getId(), user.getEmail(), user.getRole(), user.getIsActive());
    }

    @Override
    @Transactional
    public void setUserActiveStatus(Long id, boolean isActive) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + id + " не найден."));
        user.setIsActive(isActive);
        userRepository.save(user);
    }
}
