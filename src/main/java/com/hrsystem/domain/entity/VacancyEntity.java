package com.hrsystem.domain.entity;

import com.hrsystem.domain.enums.Currency;
import com.hrsystem.domain.enums.EmploymentType;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "vacancies")
public class VacancyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private EmployerProfileEntity employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private ParsingSourceEntity source;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency = Currency.RUB;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements_stack", columnDefinition = "TEXT")
    private String requirementsStack;

    @Column(name = "location", length = 100)
    private String location = "Не указано";

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 50)
    private EmploymentType employmentType = EmploymentType.REMOTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50)
    private VacancySource sourceType = VacancySource.MANUAL;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "content_hash", length = 64, unique = true)
    private String contentHash;

    @Column(name = "is_parsed", nullable = false)
    private Boolean isParsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VacancyStatus status = VacancyStatus.ACTIVE;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VacancyEntity() {
    }

    public VacancyEntity(String title, String companyName, Integer salaryMin, Integer salaryMax,
                         Currency currency, String description, String requirementsStack,
                         String location, EmploymentType employmentType, VacancySource sourceType) {
        this.title = title;
        this.companyName = companyName;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.currency = currency;
        this.description = description;
        this.requirementsStack = requirementsStack;
        this.location = location;
        this.employmentType = employmentType;
        this.sourceType = sourceType;
        this.status = VacancyStatus.ACTIVE;
        this.isParsed = false;
        this.publishedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
        if (this.status == null) {
            this.status = VacancyStatus.ACTIVE;
        }
        if (this.isParsed == null) {
            this.isParsed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String formatSalary() {
        if (salaryMin == null && salaryMax == null) {
            return "По договоренности";
        }
        String curr = (currency != null) ? currency.name() : "RUB";
        if (salaryMin != null && salaryMax != null) {
            return String.format("%,d - %,d %s", salaryMin, salaryMax, curr).replace(',', ' ');
        } else if (salaryMin != null) {
            return String.format("от %,d %s", salaryMin, curr).replace(',', ' ');
        } else {
            return String.format("до %,d %s", salaryMax, curr).replace(',', ' ');
        }
    }

    public String formatPublishedDate() {
        if (publishedAt == null) return "-";
        return DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.systemDefault())
                .format(publishedAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EmployerProfileEntity getEmployer() {
        return employer;
    }

    public void setEmployer(EmployerProfileEntity employer) {
        this.employer = employer;
    }

    public ParsingSourceEntity getSource() {
        return source;
    }

    public void setSource(ParsingSourceEntity source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Integer salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Integer salaryMax) {
        this.salaryMax = salaryMax;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirementsStack() {
        return requirementsStack;
    }

    public void setRequirementsStack(String requirementsStack) {
        this.requirementsStack = requirementsStack;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public VacancySource getSourceType() {
        return sourceType;
    }

    public void setSourceType(VacancySource sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public Boolean getIsParsed() {
        return isParsed;
    }

    public void setIsParsed(Boolean parsed) {
        isParsed = parsed;
    }

    public boolean isParsed() {
        return Boolean.TRUE.equals(isParsed);
    }

    public void setParsed(boolean parsed) {
        isParsed = parsed;
    }

    public VacancyStatus getStatus() {
        return status;
    }

    public void setStatus(VacancyStatus status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setPublishedAt(java.time.LocalDateTime ldt) {
        this.publishedAt = ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : null;
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
        VacancyEntity that = (VacancyEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VacancyEntity{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", companyName='" + companyName + '\'' +
                ", status=" + status +
                '}';
    }
}
