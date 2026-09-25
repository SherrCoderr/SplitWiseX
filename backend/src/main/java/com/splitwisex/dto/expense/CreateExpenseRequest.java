package com.splitwisex.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Incoming payload for POST /api/groups/{groupId}/expenses.
 *
 * {@code paidBy} and every id in {@code participantIds} are verified
 * against the group's membership in ExpenseService — a request can't grant
 * itself permission just by naming an id.
 */
public record CreateExpenseRequest(

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Payer is required")
        Long paidBy,

        @NotEmpty(message = "At least one participant is required")
        List<Long> participantIds
) {
}
