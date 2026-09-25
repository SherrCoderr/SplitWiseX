package com.splitwisex.exception;

/**
 * Thrown whenever an operation requires group membership that the subject
 * user doesn't have — the authenticated caller, an expense's payer, or an
 * expense participant. Mapped to HTTP 403 by GlobalExceptionHandler.
 *
 * Deliberately does not distinguish "group doesn't exist" from "you're not
 * a member of it" for the authenticated caller's own access checks — both
 * are handled as GroupNotFoundException first (see GroupService), so by the
 * time this is thrown the group is known to exist and membership is simply
 * missing.
 */
public class NotGroupMemberException extends RuntimeException {

    public NotGroupMemberException(Long groupId) {
        super("You are not a member of group " + groupId);
    }

    public NotGroupMemberException(Long groupId, String who) {
        super(who + " is not a member of group " + groupId);
    }
}
