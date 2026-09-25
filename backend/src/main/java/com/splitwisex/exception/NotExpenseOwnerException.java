package com.splitwisex.exception;

/**
 * Thrown when a group member who did not pay for an expense tries to
 * delete it. Stage 3's permission rule: only the payer can delete their own
 * expense. Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class NotExpenseOwnerException extends RuntimeException {

    public NotExpenseOwnerException(Long expenseId) {
        super("Only the person who paid for expense " + expenseId + " can delete it");
    }
}
