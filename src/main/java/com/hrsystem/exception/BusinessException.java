package com.hrsystem.exception;

/**
 * Ошибка бизнес-логики: невалидные данные, запрещённые переходы и т.д.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
