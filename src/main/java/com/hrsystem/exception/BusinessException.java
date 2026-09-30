package com.hrsystem.exception;

/**
 * Требование КР1: Собственные исключения.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
