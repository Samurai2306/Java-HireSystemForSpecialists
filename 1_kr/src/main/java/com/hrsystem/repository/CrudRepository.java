package com.hrsystem.repository;

import java.sql.SQLException;
import java.util.List;


public interface CrudRepository<T, ID> {
    void create(T entity) throws SQLException;
    List<T> findAll() throws SQLException;
    T findById(ID id) throws SQLException;
    void delete(ID id) throws SQLException;
}
