package com.splitwisex.event;

import com.splitwisex.dto.event.GroupEventType;

/**
 * Internal domain event for a group change (expense added/deleted, member
 * added). Published from inside the owning @Transactional service method
 * via GroupEventPublisher, and picked up by GroupWebSocketNotifier
 * *after* the transaction commits — see that class for why.
 */
public record GroupChangeEvent(
        GroupEventType type,
        Long groupId,
        Long entityId
) {
}
