package com.hrsystem.service;

import com.hrsystem.exception.BusinessException;
import com.hrsystem.exception.EntityNotFoundException;
import com.hrsystem.model.Vacancy;
import com.hrsystem.model.VacancyStatus;
import com.hrsystem.repository.CrudRepository;
import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.util.DatabaseManager;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors; 

public class VacancyService {
    private final CrudRepository<Vacancy, Long> repository;
    private final VacancyRepository vacancyRepository;

    public VacancyService(VacancyRepository repository) {
        this.repository = repository;
        this.vacancyRepository = repository;
    }

    public void addVacancy(String title, String companyName, java.math.BigDecimal salaryMin, java.math.BigDecimal salaryMax) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("Название пустое, так нельзя.");
        }
        if (salaryMin != null && salaryMin.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException("Зарплата не бывает отрицательной.");
        }
        if (salaryMin != null && salaryMax != null && salaryMax.compareTo(salaryMin) < 0) {
            throw new BusinessException("Верхняя граница зарплаты меньше нижней, проверьте цифры.");
        }

        Vacancy vacancy = new Vacancy(title, companyName, salaryMin, salaryMax, VacancyStatus.ACTIVE);
        try {
            repository.create(vacancy);
        } catch (SQLException e) {
            System.err.println("Не удалось сохранить вакансию: " + e.getMessage());
        }
    }

    public void printAllVacancies() {
        try {
            List<Vacancy> vacancies = repository.findAll();
            if (vacancies.isEmpty()) {
                System.out.println("Вакансий пока нет.");
                return;
            }
            
            vacancies.stream()
                    .sorted(Comparator.comparing(Vacancy::getTitle))
                    .forEach(Vacancy::printFormatted);
                    
        } catch (SQLException e) {
            System.err.println("Не получилось загрузить вакансии: " + e.getMessage());
        }
    }

    public void searchByTitle(String keyword) {
        try {
            List<Vacancy> all = repository.findAll();
            List<Vacancy> filtered = all.stream()
                    .filter(v -> v.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
            
            if (filtered.isEmpty()) {
                System.out.println("По '" + keyword + "' ничего нет.");
            } else {
                filtered.forEach(Vacancy::printFormatted);
            }
        } catch (SQLException e) {
            System.err.println("Проблема с базой: " + e.getMessage());
        }
    }
    
    public void archiveVacancy(Long id) {
        try {
            Vacancy v = repository.findById(id);
            if (v == null) {
                throw new EntityNotFoundException("Нет вакансии с ID " + id + ".");
            }
            if (v.getStatus() == VacancyStatus.ARCHIVED) {
                throw new BusinessException("Она и так в архиве.");
            }
            
            vacancyRepository.updateStatus(id, VacancyStatus.ARCHIVED.name());
            System.out.println("Перенесли в архив.");
            
        } catch (SQLException e) {
            System.err.println("Проблема с базой: " + e.getMessage());
        }
    }

    public void printStatistics() {
        System.out.println("\n=== Статистика ===");
        
        String sql = """
                SELECT 
                    (SELECT COUNT(*) FROM users) as total_users,
                    (SELECT COUNT(*) FROM vacancies) as total_vacancies,
                    (SELECT COUNT(*) FROM vacancies WHERE status = 'ACTIVE') as active_vacancies,
                    (SELECT COUNT(*) FROM vacancies WHERE source_type = 'MANUAL') as manual_vacancies,
                    (SELECT COUNT(*) FROM vacancies WHERE is_parsed = true) as parsed_vacancies
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                System.out.printf("Пользователей: %d%n", rs.getInt("total_users"));
                System.out.printf("Вакансий: %d%n", rs.getInt("total_vacancies"));
                System.out.printf("Из них активных: %d%n", rs.getInt("active_vacancies"));
                System.out.printf("Добавлено руками: %d%n", rs.getInt("manual_vacancies"));
                System.out.printf("Спарсено: %d%n", rs.getInt("parsed_vacancies"));
            }

        } catch (SQLException e) {
            System.err.println("Не удалось получить статистику: " + e.getMessage());
        }
    }

    public void exportToCsv() {
        String sql = "SELECT id, title, company_name, salary_min, salary_max, status FROM vacancies ORDER BY id";
        String filePath = "vacancies_export.csv";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery();
             PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {

            pw.println("ID;Название;Компания;Зарплата_От;Зарплата_До;Статус");

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title").replace(";", ","); 
                String company = rs.getString("company_name").replace(";", ",");
                
                java.math.BigDecimal salaryMin = rs.getBigDecimal("salary_min");
                java.math.BigDecimal salaryMax = rs.getBigDecimal("salary_max");
                String status = rs.getString("status");

                pw.printf("%d;%s;%s;%s;%s;%s%n", 
                        id, 
                        title, 
                        company, 
                        salaryMin != null ? salaryMin : "", 
                        salaryMax != null ? salaryMax : "", 
                        status);
                count++;
            }
            
            System.out.println("Выгружено " + count + " записей в " + filePath);

        } catch (Exception e) {
            System.err.println("Не получилось выгрузить CSV: " + e.getMessage());
        }
    }
}
