package com.splitwisex.controller;

import com.splitwisex.dto.group.AddMemberRequest;
import com.splitwisex.dto.group.CreateGroupRequest;
import com.splitwisex.dto.group.GroupDetailDto;
import com.splitwisex.dto.group.GroupMemberDto;
import com.splitwisex.dto.group.GroupSummaryDto;
import com.splitwisex.entity.User;
import com.splitwisex.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * All endpoints here require authentication (see SecurityConfig — anything
 * outside /api/auth/** and /api/health needs a valid bearer token). The
 * authenticated user is injected as the User principal set by
 * JwtAuthenticationFilter, and every operation is scoped to groups that
 * user actually belongs to via GroupService's membership checks.
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupDetailDto> createGroup(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupDetailDto group = groupService.createGroup(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryDto>> getMyGroups(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(groupService.getUserGroups(currentUser));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailDto> getGroup(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getGroupDetail(currentUser, groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberDto> addMember(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        GroupMemberDto member = groupService.addMember(currentUser, groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getMembers(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getMembers(currentUser, groupId));
    }
}
