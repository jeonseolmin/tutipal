package com.tutipal.domain.user.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutipal.domain.user.dto.SocialProfile;
import com.tutipal.domain.user.dto.UserResponse;
import com.tutipal.domain.user.entity.User;
import com.tutipal.domain.user.repository.UserRepository;
import com.tutipal.global.exception.ApiException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 소셜 로그인 때: 처음이면 가입, 아니면 프로필 갱신. */
    @Transactional
    public User upsert(SocialProfile profile) {
        return userRepository.findByProviderAndProviderId(profile.provider(), profile.providerId())
                .map(user -> {
                    user.updateProfile(profile.email(), profile.displayName());
                    return user;
                })
                .orElseGet(() -> userRepository.save(new User(
                        profile.provider(), profile.providerId(), profile.email(), profile.displayName())));
    }

    /** 다른 도메인(auth)용: 엔티티 그대로 (세션 로그인 만들 때). */
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    public UserResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    /** 다른 도메인(세션 참가자 목록 등)용: id → 이름. */
    public Map<Long, String> namesOf(Collection<Long> ids) {
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getName));
    }
}
