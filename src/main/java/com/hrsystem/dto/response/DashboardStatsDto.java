package com.hrsystem.dto.response;

public record DashboardStatsDto(
        long activeVacancies,
        long websiteVacancies,
        long telegramVacancies,
        long manualVacancies,
        String lastParsingStartedAt,
        String lastParsingStatus
) {
}
