package com.hrsystem.service;

import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.dto.response.UserResponseDto;

import java.util.Optional;

public interface UserService {
    Optional<UserEntity> findById(Long id);
    Optional<UserEntity> findByEmail(String email);
    UserResponseDto getUserResponse(Long id);
    void setUserActiveStatus(Long id, boolean isActive);
}
