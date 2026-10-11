package com.vinayak.secapp.user.mapper;

import com.vinayak.secapp.user.dto.UserSummaryResponse;
import com.vinayak.secapp.user.entity.User;
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
