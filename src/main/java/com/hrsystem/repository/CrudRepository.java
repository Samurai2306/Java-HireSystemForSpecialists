package com.hrsystem.repository;

import java.sql.SQLException;
import java.util.List;

/**
 * Требование КР1: Использование минимум одного интерфейса.
 * Демонстрирует абстракцию доступа к данным.
 */
public interface CrudRepository<T, ID> {
    void create(T entity) throws SQLException;
    List<T> findAll() throws SQLException;
    T findById(ID id) throws SQLException;
    void delete(ID id) throws SQLException;
}
