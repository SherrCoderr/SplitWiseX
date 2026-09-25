package com.splitwisex.dto.expense;

import com.splitwisex.dto.auth.UserSummaryDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full expense representation returned by the expense endpoints, including
 * each participant's calculated equal share.
 */
public record ExpenseDto(
        Long id,
        Long groupId,
        String description,
        BigDecimal amount,
        UserSummaryDto paidBy,
        List<ExpenseParticipantDto> participants,
        Instant createdAt
) {
}
