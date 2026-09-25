package com.splitwisex.mapper;

import com.splitwisex.dto.auth.UserSummaryDto;
import com.splitwisex.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(user.getId(), user.getName(), user.getEmail());
    }
}
