package com.hrsystem.repository;

import com.hrsystem.model.Vacancy;
import com.hrsystem.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VacancyRepository implements CrudRepository<Vacancy, Long> {

    @Override
    public void create(Vacancy vacancy) throws SQLException {
        String sql = "INSERT INTO vacancies (title, company_name, salary_min, salary_max, status, created_at, updated_at, is_parsed) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), false)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vacancy.getTitle());
            stmt.setString(2, vacancy.getCompanyName());
            stmt.setBigDecimal(3, vacancy.getSalaryMin());
            stmt.setBigDecimal(4, vacancy.getSalaryMax());
            stmt.setString(5, vacancy.getStatus().name());
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    vacancy.setId(generatedKeys.getLong(1));
                }
            }
        }
    }

    @Override
    public List<Vacancy> findAll() throws SQLException {
        List<Vacancy> list = new ArrayList<>();
        String sql = "SELECT id, title, company_name, salary_min, salary_max, status FROM vacancies";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public Vacancy findById(Long id) throws SQLException {
        String sql = "SELECT id, title, company_name, salary_min, salary_max, status FROM vacancies WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public void updateStatus(Long id, String newStatus) throws SQLException {
        String sql = "UPDATE vacancies SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM vacancies WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Vacancy mapRow(ResultSet rs) throws SQLException {
        return new Vacancy(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("company_name"),
                rs.getBigDecimal("salary_min"),
                rs.getBigDecimal("salary_max"),
                rs.getString("status")
        );
    }
}
