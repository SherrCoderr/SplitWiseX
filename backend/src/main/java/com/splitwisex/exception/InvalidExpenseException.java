package com.splitwisex.exception;

/**
 * Thrown for expense-creation business-rule failures that aren't already
 * covered by bean validation on CreateExpenseRequest (e.g. duplicate
 * participant ids collapsing to zero distinct participants). Mapped to
 * HTTP 400 by GlobalExceptionHandler.
 */
public class InvalidExpenseException extends RuntimeException {

    public InvalidExpenseException(String message) {
        super(message);
    }
}
