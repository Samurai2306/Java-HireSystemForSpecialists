package com.hrsystem.service;

import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.dto.request.UserRegistrationDto;

public interface AuthService {

    UserEntity register(UserRegistrationDto dto);

    UserEntity login(String email, String rawPassword);
}
