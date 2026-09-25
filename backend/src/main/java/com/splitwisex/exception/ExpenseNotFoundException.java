package com.splitwisex.exception;

/**
 * Thrown when an expense id doesn't correspond to any existing expense.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long expenseId) {
        super("Expense not found: " + expenseId);
    }
}
