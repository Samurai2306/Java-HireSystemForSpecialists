package com.hrsystem.repository;

import java.sql.SQLException;
import java.util.List;

/**
 * Общий интерфейс для работы с любой сущностью в БД.
 * Конкретные репозитории его реализуют.
 */
public interface CrudRepository<T, ID> {
    void create(T entity) throws SQLException;
    List<T> findAll() throws SQLException;
    T findById(ID id) throws SQLException;
    void delete(ID id) throws SQLException;
}
