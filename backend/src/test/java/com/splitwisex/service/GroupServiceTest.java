package com.splitwisex.service;

import com.splitwisex.dto.group.AddMemberRequest;
import com.splitwisex.dto.group.CreateGroupRequest;
import com.splitwisex.dto.group.GroupDetailDto;
import com.splitwisex.dto.group.GroupMemberDto;
import com.splitwisex.dto.group.GroupSummaryDto;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.GroupMember;
import com.splitwisex.entity.User;
import com.splitwisex.exception.DuplicateGroupMemberException;
import com.splitwisex.exception.GroupNotFoundException;
import com.splitwisex.exception.NotGroupMemberException;
import com.splitwisex.exception.UserNotFoundException;
import com.splitwisex.mapper.GroupMapper;
import com.splitwisex.mapper.UserMapper;
import com.splitwisex.repository.ExpenseRepository;
import com.splitwisex.repository.GroupMemberRepository;
import com.splitwisex.repository.GroupRepository;
import com.splitwisex.repository.UserRepository;
import com.splitwisex.websocket.GroupEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupEventPublisher groupEventPublisher;

    private GroupService groupService;

    private User sameer;
    private User rahul;

    @BeforeEach
    void setUp() {
        // GroupMapper/UserMapper have no dependencies of their own, so real
        // instances are simpler than mocking them.
        groupService = new GroupService(
                groupRepository, groupMemberRepository, expenseRepository, userRepository,
                new GroupMapper(new UserMapper()), groupEventPublisher
        );

        sameer = User.builder().id(1L).name("Sameer").email("sameer@example.com").passwordHash("h").build();
        rahul = User.builder().id(2L).name("Rahul").email("rahul@example.com").passwordHash("h").build();
    }

    @Test
    void creatingAGroupAutomaticallyAddsTheCreatorAsAMember() {
        CreateGroupRequest request = new CreateGroupRequest("Goa Trip", "Beach house weekend");

        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(Instant.now());
            return toSave;
        });
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> {
            GroupMember toSave = invocation.getArgument(0);
            toSave.setId(100L);
            toSave.setJoinedAt(Instant.now());
            return toSave;
        });
        // getGroupDetail() call inside createGroup() re-reads the group and members.
        when(groupRepository.findById(10L)).thenAnswer(invocation -> {
            Group group = Group.builder().id(10L).name("Goa Trip").description("Beach house weekend")
                    .createdBy(sameer).createdAt(Instant.now()).build();
            return Optional.of(group);
        });
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(
                GroupMember.builder().id(100L).user(sameer).joinedAt(Instant.now()).build()
        ));

        GroupDetailDto result = groupService.createGroup(sameer, request);

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(sameer);

        assertThat(result.name()).isEqualTo("Goa Trip");
        assertThat(result.members()).hasSize(1);
        assertThat(result.members().get(0).userId()).isEqualTo(1L);
    }

    @Test
    void userCanRetrieveTheGroupsTheyBelongTo() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findAllForUser(1L)).thenReturn(List.of(group));
        when(groupMemberRepository.countByGroupId(10L)).thenReturn(3L);
        when(expenseRepository.countByGroupId(10L)).thenReturn(5L);

        List<GroupSummaryDto> groups = groupService.getUserGroups(sameer);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).memberCount()).isEqualTo(3L);
        assertThat(groups.get(0).expenseCount()).isEqualTo(5L);
    }

    @Test
    void nonMemberCannotAccessAnotherUsersGroup() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getGroupDetail(rahul, 10L))
                .isInstanceOf(NotGroupMemberException.class);
    }

    @Test
    void accessingANonexistentGroupThrowsNotFound() {
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupDetail(sameer, 999L))
                .isInstanceOf(GroupNotFoundException.class);
    }

    @Test
    void duplicateMembershipIsPrevented() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true); // caller is a member
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(rahul));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true); // rahul already a member

        assertThatThrownBy(() -> groupService.addMember(sameer, 10L, new AddMemberRequest("rahul@example.com")))
                .isInstanceOf(DuplicateGroupMemberException.class);

        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void addingAMemberWhoDoesNotExistFails() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(sameer, 10L, new AddMemberRequest("ghost@example.com")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void aNonMemberCannotAddOtherMembersToAGroup() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> groupService.addMember(rahul, 10L, new AddMemberRequest("sameer@example.com")))
                .isInstanceOf(NotGroupMemberException.class);
    }

    @Test
    void memberCanBeAddedSuccessfully() {
        Group group = Group.builder().id(10L).name("Goa Trip").createdBy(sameer).createdAt(Instant.now()).build();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(rahul));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> {
            GroupMember toSave = invocation.getArgument(0);
            toSave.setId(200L);
            toSave.setJoinedAt(Instant.now());
            return toSave;
        });

        GroupMemberDto result = groupService.addMember(sameer, 10L, new AddMemberRequest("Rahul@Example.com"));

        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.email()).isEqualTo("rahul@example.com");
    }
}
