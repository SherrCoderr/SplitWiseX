package com.splitwisex.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming payload for POST /api/groups.
 */
public record CreateGroupRequest(

        @NotBlank(message = "Group name is required")
        @Size(max = 120, message = "Group name must be at most 120 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {
}
