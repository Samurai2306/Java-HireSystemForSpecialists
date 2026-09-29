package com.hrsystem.dto.request;

import com.hrsystem.domain.enums.UserRole;

public class UserRegistrationDto {
    private String email;
    private String password;
    private UserRole role = UserRole.CANDIDATE;
    private String fullName;
    private String targetTitle;
    private String skills;
    private String phone;
    private String telegram;
    private String portfolioLinks;
    private String companyName;
    private String contactPerson;
    private String websiteUrl;
    private String companyDescription;

    public UserRegistrationDto() {
    }

    public UserRegistrationDto(String email, String password, String fullName, String targetTitle,
                               String skills, String phone, String telegram, String portfolioLinks) {
        this.email = email;
        this.password = password;
        this.role = UserRole.CANDIDATE;
        this.fullName = fullName;
        this.targetTitle = targetTitle;
        this.skills = skills;
        this.phone = phone;
        this.telegram = telegram;
        this.portfolioLinks = portfolioLinks;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram;
    }

    public String getPortfolioLinks() {
        return portfolioLinks;
    }

    public void setPortfolioLinks(String portfolioLinks) {
        this.portfolioLinks = portfolioLinks;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }
}
