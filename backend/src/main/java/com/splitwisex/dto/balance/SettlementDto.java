package com.splitwisex.dto.balance;

import java.math.BigDecimal;

/**
 * A single "who pays whom how much" transaction, as produced by
 * SettlementCalculator and returned from GET
 * /api/groups/{groupId}/settlements. Not persisted — Stage 4 calculates
 * these dynamically from expenses on every request.
 */
public record SettlementDto(
        SettlementPartyDto from,
        SettlementPartyDto to,
        BigDecimal amount
) {
}
