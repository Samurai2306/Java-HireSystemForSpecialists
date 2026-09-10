package com.hrsystem.dto.response;

import com.hrsystem.domain.enums.UserRole;

public class UserResponseDto {
    private Long id;
    private String email;
    private UserRole role;
    private Boolean isActive;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String email, UserRole role, Boolean isActive) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
