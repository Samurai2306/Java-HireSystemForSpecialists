package com.hrsystem.exception;

/**
 * Кидаем, когда запись не нашлась в базе по id.
 */
public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
