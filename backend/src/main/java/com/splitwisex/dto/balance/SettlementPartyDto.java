package com.splitwisex.dto.balance;

/**
 * Minimal user reference used as the "from"/"to" side of a SettlementDto —
 * deliberately smaller than UserSummaryDto since a settlement only needs
 * enough to identify and label a person, not their email.
 */
public record SettlementPartyDto(
        Long id,
        String name
) {
}
