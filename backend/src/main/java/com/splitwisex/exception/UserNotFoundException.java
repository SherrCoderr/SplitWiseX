package com.splitwisex.exception;

/**
 * Thrown when a referenced user (a member to add, an expense payer, or an
 * expense participant) doesn't exist. Mapped to HTTP 404 by
 * GlobalExceptionHandler.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }

    public UserNotFoundException(String email) {
        super("No user found with email '" + email + "'");
    }
}
