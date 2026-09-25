package com.splitwisex.dto.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for POST /api/groups/{groupId}/members.
 *
 * Members are added by email rather than raw user id — the person adding
 * them generally doesn't know (and shouldn't need to know) another user's
 * internal id, only their email, the same identifier used for login.
 */
public record AddMemberRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email
) {
}
