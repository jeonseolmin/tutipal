package com.tutipal.domain.user.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.tutipal.domain.user.dto.SocialProfile;
import com.tutipal.domain.user.entity.User;

/**
 * 소셜 로그인 성공 → users 테이블에 저장 → 세션(Redis)에는 우리 DB id 와 역할만.
 * principal.getName() == 우리 users.id
 */
@Service
public class SocialOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    public SocialOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User social = super.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId();
        SocialProfile profile;
        try {
            profile = SocialProfile.of(provider, social.getAttributes());
        } catch (IllegalArgumentException e) {
            // 응답 모양이 이상하면 500 대신 로그인 실패로 처리
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), e);
        }
        return principalOf(userService.upsert(profile));
    }

    /** 세션에 넣는 로그인 정보: 우리 DB id 와 계정 역할만. (개발용 로그인도 같은 모양을 씀) */
    public static OAuth2User principalOf(User user) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                Map.of("id", String.valueOf(user.getId()), "role", user.getRole().name()),
                "id");
    }
}
