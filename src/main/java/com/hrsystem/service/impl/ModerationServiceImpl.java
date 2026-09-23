package com.hrsystem.service.impl;

import com.hrsystem.domain.entity.ParsingLogEntity;
import com.hrsystem.domain.entity.ParsingSourceEntity;
import com.hrsystem.domain.entity.UserEntity;
import com.hrsystem.domain.entity.VacancyEntity;
import com.hrsystem.domain.enums.UserRole;
import com.hrsystem.domain.enums.VacancySource;
import com.hrsystem.domain.enums.VacancyStatus;
import com.hrsystem.dto.response.DashboardStatsDto;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.repository.ParsingLogRepository;
import com.hrsystem.repository.ParsingSourceRepository;
import com.hrsystem.repository.UserRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.ModerationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ModerationServiceImpl implements ModerationService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final VacancyRepository vacancyRepository;
    private final UserRepository userRepository;
    private final ParsingSourceRepository parsingSourceRepository;
    private final ParsingLogRepository parsingLogRepository;

    public ModerationServiceImpl(VacancyRepository vacancyRepository,
                                 UserRepository userRepository,
                                 ParsingSourceRepository parsingSourceRepository,
                                 ParsingLogRepository parsingLogRepository) {
        this.vacancyRepository = vacancyRepository;
        this.userRepository = userRepository;
        this.parsingSourceRepository = parsingSourceRepository;
        this.parsingLogRepository = parsingLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setActiveVacancies(vacancyRepository.countByStatus(VacancyStatus.ACTIVE));
        stats.setWebsiteVacancies(vacancyRepository.countByStatusAndSourceType(VacancyStatus.ACTIVE, VacancySource.WEBSITE));
        stats.setTelegramVacancies(vacancyRepository.countByStatusAndSourceType(VacancyStatus.ACTIVE, VacancySource.TELEGRAM));
        stats.setManualVacancies(vacancyRepository.countByStatusAndSourceType(VacancyStatus.ACTIVE, VacancySource.MANUAL));

        Optional<ParsingLogEntity> lastLog = parsingLogRepository.findTopByOrderByStartedAtDesc();
        if (lastLog.isEmpty()) {
            stats.setLastParsingStartedAt("ещё не запускался");
            stats.setLastParsingStatus("—");
            return stats;
        }
        ParsingLogEntity log = lastLog.get();
        if (log.getStartedAt() == null) {
            stats.setLastParsingStartedAt("—");
        } else {
            stats.setLastParsingStartedAt(TS.format(log.getStartedAt()));
        }
        if (log.getStatus() == null) {
            stats.setLastParsingStatus("—");
        } else {
            stats.setLastParsingStatus(log.getStatus());
        }
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VacancyEntity> listRecentVacancies() {
        return vacancyRepository.findTop50ByOrderByPublishedAtDesc();
    }

    @Override
    public VacancyEntity changeVacancyStatus(Long vacancyId, VacancyStatus status) {
        Optional<VacancyEntity> found = vacancyRepository.findById(vacancyId);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Вакансия #" + vacancyId + " не найдена");
        }
        VacancyEntity vacancy = found.get();
        vacancy.setStatus(status);
        return vacancyRepository.save(vacancy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserEntity> listUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Override
    public UserEntity setUserActive(Long userId, boolean active) {
        Optional<UserEntity> found = userRepository.findById(userId);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Пользователь #" + userId + " не найден");
        }
        UserEntity user = found.get();
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Нельзя блокировать учётную запись администратора");
        }
        user.setActive(active);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParsingSourceEntity> listSources() {
        return parsingSourceRepository.findAllByOrderByIdAsc();
    }

    @Override
    public ParsingSourceEntity addSource(String name, VacancySource sourceType, String baseUrl) {
        if (sourceType == VacancySource.MANUAL) {
            throw new IllegalArgumentException("Источник сбора может быть только WEBSITE или TELEGRAM");
        }
        if (parsingSourceRepository.existsByBaseUrlIgnoreCase(baseUrl)) {
            throw new IllegalArgumentException("Источник с таким URL уже существует");
        }
        ParsingSourceEntity source = new ParsingSourceEntity();
        source.setName(name.trim());
        source.setSourceType(sourceType);
        source.setBaseUrl(baseUrl.trim());
        source.setActive(true);
        return parsingSourceRepository.save(source);
    }

    @Override
    public ParsingSourceEntity toggleSource(Long sourceId) {
        Optional<ParsingSourceEntity> found = parsingSourceRepository.findById(sourceId);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Источник #" + sourceId + " не найден");
        }
        ParsingSourceEntity source = found.get();
        source.setActive(!source.isActive());
        return parsingSourceRepository.save(source);
    }
}
