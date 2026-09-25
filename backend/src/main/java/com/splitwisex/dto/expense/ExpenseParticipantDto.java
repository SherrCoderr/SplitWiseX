package com.splitwisex.dto.expense;

import java.math.BigDecimal;

/**
 * A participant's equal share of an expense. {@code share} is calculated on
 * read (see ExpenseMapper#calculateEqualShares) — it is never persisted.
 */
public record ExpenseParticipantDto(
        Long userId,
        String name,
        String email,
        BigDecimal share
) {
}
