package com.splitwisex.mapper;

import com.splitwisex.dto.group.GroupDetailDto;
import com.splitwisex.dto.group.GroupMemberDto;
import com.splitwisex.dto.group.GroupSummaryDto;
import com.splitwisex.entity.Group;
import com.splitwisex.entity.GroupMember;
import com.splitwisex.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupMapper {

    private final UserMapper userMapper;

    public GroupMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public GroupSummaryDto toSummary(Group group, long memberCount, long expenseCount) {
        return new GroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                memberCount,
                expenseCount,
                group.getCreatedAt()
        );
    }

    public GroupDetailDto toDetail(Group group, List<GroupMember> members) {
        List<GroupMemberDto> memberDtos = members.stream().map(this::toMemberDto).toList();
        return new GroupDetailDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                userMapper.toSummary(group.getCreatedBy()),
                group.getCreatedAt(),
                memberDtos
        );
    }

    public GroupMemberDto toMemberDto(GroupMember member) {
        User user = member.getUser();
        return new GroupMemberDto(user.getId(), user.getName(), user.getEmail(), member.getJoinedAt());
    }
}
