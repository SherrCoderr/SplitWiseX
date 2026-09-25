package com.splitwisex.dto.group;

import java.time.Instant;

/**
 * A single group member as returned by the API — never the raw User/
 * GroupMember entities.
 */
public record GroupMemberDto(
        Long userId,
        String name,
        String email,
        Instant joinedAt
) {
}
