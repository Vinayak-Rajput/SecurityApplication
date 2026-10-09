package com.hdfclife.capstoneone.user.mapper;

import com.hdfclife.capstoneone.user.dto.UserSummaryResponse;
import com.hdfclife.capstoneone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
