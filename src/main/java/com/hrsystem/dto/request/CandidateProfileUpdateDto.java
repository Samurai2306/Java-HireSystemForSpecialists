package com.hrsystem.dto.request;

public class CandidateProfileUpdateDto {
    private String fullName;
    private String targetTitle;
    private String skills;
    private String phone;
    private String telegram;
    private String portfolioLinks;

    public CandidateProfileUpdateDto() {
    }

    public CandidateProfileUpdateDto(String fullName, String targetTitle, String skills, String phone, String telegram, String portfolioLinks) {
        this.fullName = fullName;
        this.targetTitle = targetTitle;
        this.skills = skills;
        this.phone = phone;
        this.telegram = telegram;
        this.portfolioLinks = portfolioLinks;
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
}
