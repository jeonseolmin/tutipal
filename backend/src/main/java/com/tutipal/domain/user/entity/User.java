package com.tutipal.domain.user.entity;

import com.tutipal.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerId"}))
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;        // google | naver | kakao

    @Column(nullable = false, length = 100)
    private String providerId;      // 소셜 쪽 사용자 id

    private String email;           // 동의 안 하면 null 일 수 있음

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected User() {
    }

    public User(String provider, String providerId, String email, String name) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.role = Role.USER;      // 선생님/학생은 세션마다 정해짐 (studysession)
    }

    /** 로그인할 때마다 소셜 쪽 최신 이름/이메일로 갱신. */
    public void updateProfile(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getProvider() { return provider; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Role getRole() { return role; }
}
