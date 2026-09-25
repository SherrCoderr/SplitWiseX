package com.splitwisex.dto.event;

/**
 * The kinds of group changes Stage 6 notifies connected clients about.
 * Kept intentionally small — this is a notification, not an activity feed.
 */
public enum GroupEventType {
    EXPENSE_CREATED,
    EXPENSE_DELETED,
    MEMBER_ADDED
}
