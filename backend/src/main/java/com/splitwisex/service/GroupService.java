package com.splitwisex.service;

import com.splitwisex.dto.group.AddMemberRequest;
import com.splitwisex.dto.group.CreateGroupRequest;
import com.splitwisex.dto.group.GroupDetailDto;
import com.splitwisex.dto.group.GroupMemberDto;
import com.splitwisex.dto.group.GroupSummaryDto;
import com.splitwisex.dto.event.GroupEventType;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.GroupMember;
import com.splitwisex.entity.User;
import com.splitwisex.exception.DuplicateGroupMemberException;
import com.splitwisex.exception.GroupNotFoundException;
import com.splitwisex.exception.NotGroupMemberException;
import com.splitwisex.exception.UserNotFoundException;
import com.splitwisex.mapper.GroupMapper;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import com.splitwisex.repository.GroupRepository;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.websocket.GroupEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns group creation/lookup and membership. Class-level readOnly
 * transactions keep the Hibernate session open through DTO mapping (which
 * touches lazy associations like Group.createdBy and GroupMember.user);
 * the individual write methods below override that with their own
 * read-write @Transactional.
 *
 * getGroupOrThrow/assertMembership are also used by ExpenseService so that
 * "does this group exist" / "is this user a member of it" is checked the
 * same way everywhere expense operations need it.
 */
@Service
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;
    private final GroupEventPublisher groupEventPublisher;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            GroupMapper groupMapper,
            GroupEventPublisher groupEventPublisher
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.groupMapper = groupMapper;
        this.groupEventPublisher = groupEventPublisher;
    }

    @Transactional
    public GroupDetailDto createGroup(User currentUser, CreateGroupRequest request) {
        Group group = Group.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .createdBy(currentUser)
                .build();
        Group saved = groupRepository.save(group);

        // The creator automatically becomes a member of their own group.
        GroupMember membership = GroupMember.builder()
                .group(saved)
                .user(currentUser)
                .build();
        groupMemberRepository.save(membership);

        return getGroupDetail(currentUser, saved.getId());
    }

    public List<GroupSummaryDto> getUserGroups(User currentUser) {
        return groupRepository.findAllForUser(currentUser.getId()).stream()
                .map(group -> groupMapper.toSummary(
                        group,
                        groupMemberRepository.countByGroupId(group.getId()),
                        expenseRepository.countByGroupId(group.getId())
                ))
                .toList();
    }

    public GroupDetailDto getGroupDetail(User currentUser, Long groupId) {
        Group group = getGroupOrThrow(groupId);
        assertMembership(groupId, currentUser.getId());
        List<GroupMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId);
        return groupMapper.toDetail(group, members);
    }

    @Transactional
    public GroupMemberDto addMember(User currentUser, Long groupId, AddMemberRequest request) {
        Group group = getGroupOrThrow(groupId);
        assertMembership(groupId, currentUser.getId());

        String normalizedEmail = request.email().trim().toLowerCase();
        User userToAdd = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userToAdd.getId())) {
            throw new DuplicateGroupMemberException(userToAdd.getEmail());
        }

        GroupMember member = GroupMember.builder().group(group).user(userToAdd).build();
        GroupMember saved = groupMemberRepository.save(member);

        groupEventPublisher.publish(GroupEventType.MEMBER_ADDED, groupId, saved.getId());

        return groupMapper.toMemberDto(saved);
    }

    public List<GroupMemberDto> getMembers(User currentUser, Long groupId) {
        getGroupOrThrow(groupId);
        assertMembership(groupId, currentUser.getId());
        return groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(groupMapper::toMemberDto)
                .toList();
    }

    public Group getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    public void assertMembership(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new NotGroupMemberException(groupId);
        }
    }
}
