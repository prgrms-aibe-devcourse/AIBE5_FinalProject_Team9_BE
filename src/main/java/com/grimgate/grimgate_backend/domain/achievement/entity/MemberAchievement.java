package com.grimgate.grimgate_backend.domain.achievement.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원-업적 매핑 엔티티
 */
@Entity
@Table(
        name = "member_achievement",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "achievement_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    // 업적 획득 일시
    @Column(nullable = false)
    private LocalDateTime acquiredAt;
}
