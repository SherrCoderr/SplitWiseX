package com.splitwisex.dto.group;

import com.splitwisex.dto.auth.UserSummaryDto;

import java.time.Instant;
import java.util.List;

/**
 * Full group representation for GET /api/groups/{groupId}, including the
 * member list. Expenses are fetched separately via the expenses endpoints
 * rather than embedded here, to keep this response bounded in size.
 */
public record GroupDetailDto(
        Long id,
        String name,
        String description,
        UserSummaryDto createdBy,
        Instant createdAt,
        List<GroupMemberDto> members
) {
}
