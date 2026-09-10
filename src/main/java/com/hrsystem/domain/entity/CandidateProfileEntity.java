package com.hrsystem.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "target_title", length = 255)
    private String targetTitle;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "phone", length = 100)
    private String phone;

    @Column(name = "telegram", length = 100)
    private String telegram;

    @Column(name = "portfolio_links", columnDefinition = "TEXT")
    private String portfolioLinks;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CandidateProfileEntity() {
    }

    public CandidateProfileEntity(UserEntity user, String fullName, String targetTitle, String skills, String phone, String telegram) {
        this.user = user;
        this.fullName = fullName;
        this.targetTitle = targetTitle;
        this.skills = skills;
        this.phone = phone;
        this.telegram = telegram;
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateProfileEntity that = (CandidateProfileEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CandidateProfileEntity{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", targetTitle='" + targetTitle + '\'' +
                ", skills='" + skills + '\'' +
                ", phone='" + phone + '\'' +
                ", telegram='" + telegram + '\'' +
                '}';
    }
}
