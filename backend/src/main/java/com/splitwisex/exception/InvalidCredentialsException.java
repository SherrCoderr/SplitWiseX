package com.splitwisex.exception;

/**
 * Thrown when login fails, for either a nonexistent email or a wrong
 * password. Deliberately generic — never reveals which of the two was
 * wrong, so the endpoint can't be used to enumerate registered emails.
 * Mapped to HTTP 401 Unauthorized by GlobalExceptionHandler.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
