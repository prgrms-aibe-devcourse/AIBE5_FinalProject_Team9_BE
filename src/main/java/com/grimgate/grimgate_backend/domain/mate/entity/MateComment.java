package com.grimgate.grimgate_backend.domain.mate.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * 메이트 모집글 댓글 엔티티.
 *
 * <pre>
 * Table mate_comment {
 *   id           bigint   PK
 *   mate_post_id bigint   NOT NULL  FK -> mate_post.id
 *   member_id    bigint   NOT NULL  FK -> member.id
 *   content      text     NOT NULL
 *   created_at   datetime NOT NULL
 *   updated_at   datetime
 *   deleted_at   datetime
 * }
 * </pre>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "mate_comment",
        indexes = {
                @Index(name = "idx_mate_comment_post", columnList = "mate_post_id"),
                @Index(name = "idx_mate_comment_member", columnList = "member_id")
        }
)
public class MateComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 댓글이 달린 모집글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mate_post_id", nullable = false)
    private MatePost matePost;

    /** 댓글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** soft delete */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== 도메인 메서드 =====

    /** 댓글 작성자 본인 여부 검증 */
    public boolean isAuthor(Long memberId) {
        return this.member != null && this.member.getId().equals(memberId);
    }

    /** 댓글 내용 수정 */
    public void updateContent(String content) {
        this.content = content;
    }

    /** soft delete */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
