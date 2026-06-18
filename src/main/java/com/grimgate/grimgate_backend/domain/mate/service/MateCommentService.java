package com.grimgate.grimgate_backend.domain.mate.service;

import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.repository.MateCommentRepository;
import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메이트 모집글 댓글 서비스.
 *
 * <p>인증 사용자 식별</p>
 * <ul>
 *   <li>Controller 가 {@code SecurityUtil.getCurrentAccountId()} 로 accountId 를 추출하여 전달</li>
 *   <li>{@code resolveMember(accountId)} 로 Member 를 조회하여 소유권 검증에 활용</li>
 * </ul>
 *
 * <p>댓글 정책</p>
 * <ul>
 *   <li>작성: 로그인 사용자만 가능</li>
 *   <li>수정/삭제: 댓글 작성자 본인만 가능</li>
 *   <li>삭제: Soft Delete (deletedAt 설정)</li>
 *   <li>조회: deletedAt 이 null 인 댓글만 반환</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MateCommentService {

    private final MateCommentRepository commentRepository;
    private final MatePostRepository matePostRepository;
    private final MemberRepository memberRepository;

    /* ===== 조회 ===== */

    /** 특정 모집글의 댓글 목록 조회 (삭제된 댓글 제외, 작성 순 정렬) */
    public MateCommentListResponse list(Long postId) {
        getActivePost(postId); // 모집글 존재 여부 검증
        List<MateCommentResponse> comments = commentRepository
                .findActiveByMatePostId(postId)
                .stream()
                .map(MateCommentResponse::of)
                .toList();
        return MateCommentListResponse.of(comments);
    }

    /* ===== 작성 ===== */

    @Transactional
    public MateCommentResponse create(Long accountId, Long postId, MateCommentCreateRequest req) {
        Member author = resolveMember(accountId);
        MatePost post = getActivePost(postId);

        MateComment comment = MateComment.builder()
                .matePost(post)
                .member(author)
                .content(req.getContent())
                .build();

        commentRepository.save(comment);
        return MateCommentResponse.of(comment);
    }

    /* ===== 수정 ===== */

    @Transactional
    public MateCommentResponse update(Long accountId, Long postId, Long commentId,
            MateCommentUpdateRequest req) {
        Member author = resolveMember(accountId);
        getActivePost(postId); // 모집글 존재 여부 검증

        MateComment comment = getActiveComment(commentId);
        if (!comment.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_COMMENT_FORBIDDEN);
        }

        comment.updateContent(req.getContent());
        commentRepository.flush();
        return MateCommentResponse.of(comment);
    }

    /* ===== 삭제 ===== */

    @Transactional
    public void delete(Long accountId, Long postId, Long commentId) {
        Member author = resolveMember(accountId);
        getActivePost(postId); // 모집글 존재 여부 검증

        MateComment comment = getActiveComment(commentId);
        if (!comment.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_COMMENT_FORBIDDEN);
        }

        comment.softDelete();
    }

    // ===== 내부 헬퍼 =====

    /** accountId 로 Member 조회 — 없으면 MEMBER_NOT_FOUND */
    private Member resolveMember(Long accountId) {
        return memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /** 삭제되지 않은 모집글 조회 — 없으면 MATE_POST_NOT_FOUND */
    private MatePost getActivePost(Long postId) {
        return matePostRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_POST_NOT_FOUND));
    }

    /** 삭제되지 않은 댓글 단건 조회 — 없으면 MATE_COMMENT_NOT_FOUND */
    private MateComment getActiveComment(Long commentId) {
        return commentRepository.findWithMemberById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_COMMENT_NOT_FOUND));
    }
}
