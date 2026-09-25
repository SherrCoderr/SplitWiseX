package com.splitwisex.dto.auth;

/**
 * User fields that are safe to send to the client — never the password hash.
 */
public record UserSummaryDto(
        Long id,
        String name,
        String email
) {
}
