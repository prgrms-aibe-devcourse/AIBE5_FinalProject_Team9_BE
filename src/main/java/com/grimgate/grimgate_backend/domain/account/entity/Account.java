package com.grimgate.grimgate_backend.domain.account.entity;

import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@SQLRestriction("deleted_at is null")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String gender;

    private Integer age;

    @Column(nullable = true)
    private String provider;

    @Builder.Default
    @Column(nullable = false)
    private boolean notificationEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean ageVisible = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean genderVisible = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailVisible = true;

    private LocalDateTime deletedAt;

    /**
     * 프로필 수정: null이 아닌 파라미터만 해당 필드 업데이트
     */
    public void updateProfile(String nickname, Integer age, String gender,
                              Boolean ageVisible, Boolean genderVisible, Boolean emailVisible) {
        if (nickname != null) this.nickname = nickname;
        if (age != null) this.age = age;
        if (gender != null) this.gender = gender;
        if (ageVisible != null) this.ageVisible = ageVisible;
        if (genderVisible != null) this.genderVisible = genderVisible;
        if (emailVisible != null) this.emailVisible = emailVisible;
    }

    // 비밀번호 변경
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 회원 탈퇴 처리: deletedAt 세팅 + email unique 제약 충돌 방지를 위해 이메일 변조
     * 변조 형식: 원본이메일_deleted_계정ID
     */
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.email = this.email + "_deleted_" + this.id;
    }
}
