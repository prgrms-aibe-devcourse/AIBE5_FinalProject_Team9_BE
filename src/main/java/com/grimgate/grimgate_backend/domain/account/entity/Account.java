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

    @Column(nullable = false)
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

    private LocalDateTime deletedAt;
}
