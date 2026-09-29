package com.hrsystem.domain.entity;

import com.hrsystem.domain.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "applications")
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private VacancyEntity vacancy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfileEntity candidate;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "status_comment", columnDefinition = "TEXT")
    private String statusComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ApplicationEntity() {
    }

    public ApplicationEntity(VacancyEntity vacancy, CandidateProfileEntity candidate, String coverLetter) {
        this.vacancy = vacancy;
        this.candidate = candidate;
        this.coverLetter = coverLetter;
        this.status = ApplicationStatus.APPLIED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ApplicationStatus.APPLIED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String formatCreatedAt() {
        if (createdAt == null) return "-";
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VacancyEntity getVacancy() {
        return vacancy;
    }

    public void setVacancy(VacancyEntity vacancy) {
        this.vacancy = vacancy;
    }

    public CandidateProfileEntity getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateProfileEntity candidate) {
        this.candidate = candidate;
    }

    public CandidateProfileEntity getCandidateProfile() {
        return candidate;
    }

    public void setCandidateProfile(CandidateProfileEntity candidateProfile) {
        this.candidate = candidateProfile;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getStatusComment() {
        return statusComment;
    }

    public void setStatusComment(String statusComment) {
        this.statusComment = statusComment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
        ApplicationEntity that = (ApplicationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ApplicationEntity{" +
                "id=" + id +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
