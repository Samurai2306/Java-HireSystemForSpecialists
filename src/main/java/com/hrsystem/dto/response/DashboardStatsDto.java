package com.hrsystem.dto.response;

public class DashboardStatsDto {
    private long activeVacancies;
    private long websiteVacancies;
    private long telegramVacancies;
    private long manualVacancies;
    private String lastParsingStartedAt;
    private String lastParsingStatus;

    public long getActiveVacancies() {
        return activeVacancies;
    }

    public void setActiveVacancies(long activeVacancies) {
        this.activeVacancies = activeVacancies;
    }

    public long getWebsiteVacancies() {
        return websiteVacancies;
    }

    public void setWebsiteVacancies(long websiteVacancies) {
        this.websiteVacancies = websiteVacancies;
    }

    public long getTelegramVacancies() {
        return telegramVacancies;
    }

    public void setTelegramVacancies(long telegramVacancies) {
        this.telegramVacancies = telegramVacancies;
    }

    public long getManualVacancies() {
        return manualVacancies;
    }

    public void setManualVacancies(long manualVacancies) {
        this.manualVacancies = manualVacancies;
    }

    public String getLastParsingStartedAt() {
        return lastParsingStartedAt;
    }

    public void setLastParsingStartedAt(String lastParsingStartedAt) {
        this.lastParsingStartedAt = lastParsingStartedAt;
    }

    public String getLastParsingStatus() {
        return lastParsingStatus;
    }

    public void setLastParsingStatus(String lastParsingStatus) {
        this.lastParsingStatus = lastParsingStatus;
    }
}
