package com.splitwisex.dto.group;

import java.time.Instant;

/**
 * Lightweight group representation for list views (GET /api/groups) —
 * counts instead of the full member/expense lists, which GroupDetailDto
 * carries.
 */
public record GroupSummaryDto(
        Long id,
        String name,
        String description,
        long memberCount,
        long expenseCount,
        Instant createdAt
) {
}
