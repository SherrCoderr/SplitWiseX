package com.splitwisex.exception;

/**
 * Thrown when a registration attempt uses an email that already has an
 * account. Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
