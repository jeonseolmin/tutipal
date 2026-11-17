package com.tutipal.domain.user.dto;

import com.tutipal.domain.user.entity.Role;
import com.tutipal.domain.user.entity.User;

/** 화면으로 나가는 사용자 정보 (엔티티를 직접 내보내지 않기). */
public record UserResponse(Long id, String name, String email, String provider, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getProvider(), user.getRole());
    }
}
