package com.splitwisex.exception;

/**
 * Thrown when a group id doesn't correspond to any existing group. Mapped
 * to HTTP 404 by GlobalExceptionHandler.
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(Long groupId) {
        super("Group not found: " + groupId);
    }
}
