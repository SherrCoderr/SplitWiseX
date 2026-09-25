package com.splitwisex.dto.event;

import java.time.Instant;

/**
 * The message body clients receive on /topic/groups/{groupId}.
 *
 * Deliberately minimal — just enough for the frontend to know *that*
 * something changed and where, not the changed data itself. The frontend
 * always refetches the authoritative REST resource in response; this is
 * never treated as the source of truth for balances or anything else.
 */
public record GroupEventMessage(
        GroupEventType type,
        Long groupId,
        Long entityId,
        Instant timestamp
) {
}
