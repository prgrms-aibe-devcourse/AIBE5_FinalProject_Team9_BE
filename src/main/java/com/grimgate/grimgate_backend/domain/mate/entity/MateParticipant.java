package com.grimgate.grimgate_backend.domain.mate.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 참여자 엔티티.
 *
 * <p>ERD 기준 컬럼</p>
 * <pre>
 * Table mate_participant {
 *   id            bigint  PK
 *   mate_post_id  bigint  NOT NULL  FK -> mate_post.id
 *   member_id     bigint  NOT NULL  FK -> member.id
 *   status        varchar NOT NULL   // JOINED / CANCELLED / KICKED
 *   joined_at     datetime NOT NULL
 *   cancelled_at  datetime
 *   updated_at    datetime
 * }
 * </pre>
 *
 * <p>제약</p>
 * <ul>
 *   <li>같은 모집글에 같은 회원이 두 번 활성 상태로 들어올 수 없도록 unique 제약(mate_post_id + member_id)</li>
 *   <li>취소 후 재참가는 같은 row 의 status 를 다시 JOINED 로 되돌리는 방식으로 처리</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "mate_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mate_participant_post_member",
                        columnNames = {"mate_post_id", "member_id"}
                )
        },
        indexes = {
                @Index(name = "idx_mate_participant_post", columnList = "mate_post_id"),
                @Index(name = "idx_mate_participant_member", columnList = "member_id")
        }
)
public class MateParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 참여 대상 모집글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mate_post_id", nullable = false)
    private MatePost matePost;

    /** 참여자 (member.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MateParticipantStatus status;

    /** 최초 참여 시각 (ERD: joined_at NOT NULL) */
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /** 마지막 취소 시각 (ERD: cancelled_at NULL 허용) */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ===== 정적 생성자 =====

    public static MateParticipant join(MatePost matePost, Member member) {
        return MateParticipant.builder()
                .matePost(matePost)
                .member(member)
                .status(MateParticipantStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // ===== 도메인 메서드 =====

    /** 참여 취소 (본인이 나감) */
    public void cancel() {
        this.status = MateParticipantStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /** 강퇴 (작성자가 내보냄) */
    public void kick() {
        this.status = MateParticipantStatus.KICKED;
        this.cancelledAt = LocalDateTime.now();
    }

    /** 취소/강퇴 후 재참가 — 동일 row 의 status 를 JOINED 로 되돌림 */
    public void rejoin() {
        this.status = MateParticipantStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
        this.cancelledAt = null;
    }

    public boolean isActive() {
        return this.status == MateParticipantStatus.JOINED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.member != null && this.member.getId().equals(memberId);
    }
}
