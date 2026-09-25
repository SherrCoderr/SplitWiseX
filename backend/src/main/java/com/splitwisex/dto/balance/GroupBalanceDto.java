package com.splitwisex.dto.balance;

import java.util.List;

/**
 * Response body for GET /api/groups/{groupId}/balances.
 */
public record GroupBalanceDto(
        Long groupId,
        List<MemberBalanceDto> members
) {
}
