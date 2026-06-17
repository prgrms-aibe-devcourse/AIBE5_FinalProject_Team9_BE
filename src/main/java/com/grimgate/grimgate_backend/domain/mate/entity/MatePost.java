package com.grimgate.grimgate_backend.domain.mate.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 엔티티.
 *
 * ERD 컬럼명을 그대로 따른다. 응답 DTO 변환 시점에 카멜케이스로 매핑한다.
 *
 * <pre>
 * Table mate_post {
 *   id                bigint  PK
 *   member_id         bigint  NOT NULL  FK -> member.id
 *   theme_id          bigint  NOT NULL  FK -> theme.id
 *   title             varchar NOT NULL
 *   content           text
 *   image_url         varchar
 *   meeting_time      datetime NOT NULL
 *   deadline          datetime
 *   current_people    int     NOT NULL
 *   max_people        int     NOT NULL
 *   tags              text
 *   experience_level  varchar
 *   open_chat_url     varchar NOT NULL
 *   status            varchar NOT NULL
 *   created_at        datetime NOT NULL
 *   updated_at        datetime
 *   deleted_at        datetime
 * }
 * </pre>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "mate_post")
public class MatePost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 (member.id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 대상 테마 (theme.id). 테마를 통해 branch까지 접근 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    /** 모임 시간 (ERD: meeting_time datetime) */
    @Column(name = "meeting_time", nullable = false)
    private LocalDateTime meetingTime;

    /** 모집 마감일 (선택) */
    @Column(name = "deadline")
    private LocalDateTime deadline;

    /** 현재 참여 인원 (작성자 포함 — 생성 시 1로 초기화) */
    @Column(name = "current_people", nullable = false)
    private Integer currentPeople;

    /** 최대 인원 */
    @Column(name = "max_people", nullable = false)
    private Integer maxPeople;

    /** 분위기 태그 (콤마로 직렬화된 문자열) */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 20)
    private ExperienceLevel experienceLevel;

    /** 카카오 오픈채팅 URL (필수) */
    @Column(name = "open_chat_url", nullable = false)
    private String openChatUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatePostStatus status;

    /** soft delete */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== 도메인 메서드 =====

    /** 작성자 본인 여부 검증 */
    public boolean isAuthor(Long memberId) {
        return this.member != null && this.member.getId().equals(memberId);
    }

    /** 모집글 수정 — null 값은 무시 */
    public void update(String title,
                       String content,
                       LocalDateTime meetingTime,
                       LocalDateTime deadline,
                       Integer maxPeople,
                       String tags,
                       ExperienceLevel experienceLevel,
                       String openChatUrl,
                       String imageUrl) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (meetingTime != null) this.meetingTime = meetingTime;
        if (deadline != null) this.deadline = deadline;
        if (maxPeople != null) this.maxPeople = maxPeople;
        if (tags != null) this.tags = tags;
        if (experienceLevel != null) this.experienceLevel = experienceLevel;
        if (openChatUrl != null) this.openChatUrl = openChatUrl;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }

    /** 참가 신청 — 인원 +1, 인원 충족 시 MATCHED 자동 전이 */
    public void increaseParticipant() {
        this.currentPeople = this.currentPeople + 1;
        if (this.currentPeople.equals(this.maxPeople)) {
            this.status = MatePostStatus.MATCHED;
        }
    }

    /** 참가 취소 — 인원 -1, MATCHED였다면 RECRUITING으로 복귀 */
    public void decreaseParticipant() {
        if (this.currentPeople <= 0) return;
        this.currentPeople = this.currentPeople - 1;
        if (this.status == MatePostStatus.MATCHED) {
            this.status = MatePostStatus.RECRUITING;
        }
    }

    /** 모집 가능 상태인지 (참가/취소 허용 상태) */
    public boolean isRecruitable() {
        return this.status == MatePostStatus.RECRUITING
                || this.status == MatePostStatus.CLOSING_SOON;
    }

    public boolean isDeleted() {
        return this.deletedAt != null || this.status == MatePostStatus.DELETED;
    }

    /** 작성자 수동 마감 — 기존 참여자는 유지하고 신규 참여만 차단 */
    public void close() {
        this.status = MatePostStatus.CLOSED;
    }

    /** soft delete */
    public void softDelete() {
        this.status = MatePostStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
