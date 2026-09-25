package com.splitwisex.exception;

/**
 * Thrown when trying to add a user to a group they already belong to.
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class DuplicateGroupMemberException extends RuntimeException {

    public DuplicateGroupMemberException(String email) {
        super("'" + email + "' is already a member of this group");
    }
}
