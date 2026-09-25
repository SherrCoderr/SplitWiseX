package com.splitwisex.dto.balance;

import java.math.BigDecimal;

/**
 * One group member's running total for GET /api/groups/{groupId}/balances.
 *
 * netBalance = totalPaid - totalShare
 *   > 0  -> this member should receive money
 *   < 0  -> this member owes money
 *   = 0  -> this member is settled up
 */
public record MemberBalanceDto(
        Long userId,
        String name,
        BigDecimal totalPaid,
        BigDecimal totalShare,
        BigDecimal netBalance
) {
}
