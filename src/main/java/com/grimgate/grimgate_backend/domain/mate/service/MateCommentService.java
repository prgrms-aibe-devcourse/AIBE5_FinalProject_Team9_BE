package com.grimgate.grimgate_backend.domain.mate.service;

import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MateReplyResponse;
import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.repository.MateCommentRepository;
import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메이트 모집글 댓글/대댓글 서비스.
 *
 * <p>대댓글 정책</p>
 * <ul>
 *   <li>1-depth만 허용: 대댓글에는 대댓글을 달 수 없다.</li>
 *   <li>삭제된 원댓글에는 대댓글을 달 수 없다.</li>
 *   <li>원댓글이 삭제되어도 대댓글이 존재하면 마스킹 처리 후 목록에 포함한다.</li>
 *   <li>삭제된 대댓글은 목록에서 제외한다.</li>
 * </ul>
 *
 * <p>totalCount 기준: 삭제되지 않은 원댓글 + 대댓글 전체 합산</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MateCommentService {

    private final MateCommentRepository commentRepository;
    private final MatePostRepository matePostRepository;
    private final MemberRepository memberRepository;

    /* ===== 조회 ===== */

    /**
     * 특정 모집글의 댓글 목록 조회.
     * 원댓글 + replies 트리 구조로 반환하며, 삭제된 원댓글은 대댓글이 있는 경우 마스킹 처리.
     */
    public MateCommentListResponse list(Long postId) {
        getActivePost(postId);

        List<MateComment> all = commentRepository.findAllByMatePostId(postId);

        // 삭제되지 않은 대댓글을 부모 ID 기준으로 그루핑
        Map<Long, List<MateComment>> repliesMap = all.stream()
                .filter(c -> c.isReply() && !c.isDeleted())
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 원댓글만 필터링 후 응답 조립
        // 삭제된 원댓글은 대댓글이 있는 경우에만 마스킹하여 포함
        List<MateCommentResponse> comments = all.stream()
                .filter(c -> !c.isReply())
                .filter(c -> !c.isDeleted() || repliesMap.containsKey(c.getId()))
                .map(c -> {
                    List<MateReplyResponse> replies = repliesMap
                            .getOrDefault(c.getId(), List.of())
                            .stream()
                            .map(MateReplyResponse::of)
                            .toList();
                    return MateCommentResponse.of(c, replies);
                })
                .toList();

        // totalCount = 삭제되지 않은 원댓글 + 대댓글 전체 합산
        int totalCount = (int) all.stream().filter(c -> !c.isDeleted()).count();

        return MateCommentListResponse.of(comments, totalCount);
    }

    /* ===== 원댓글 작성 ===== */

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

    /* ===== 대댓글 작성 ===== */

    /**
     * 대댓글 작성.
     *
     * <p>검증 순서</p>
     * <ol>
     *   <li>부모 댓글 존재 여부</li>
     *   <li>부모 댓글이 대댓글인지 (1-depth 제한)</li>
     *   <li>부모 댓글이 같은 게시글 소속인지</li>
     *   <li>부모 댓글이 삭제되지 않았는지</li>
     * </ol>
     */
    @Transactional
    public MateReplyResponse createReply(Long accountId, Long postId, Long parentCommentId,
            MateCommentCreateRequest req) {
        Member author = resolveMember(accountId);
        MatePost post = getActivePost(postId);

        // 부모 댓글 조회 (삭제 포함 — 삭제 여부는 아래에서 별도 검증)
        MateComment parent = commentRepository.findWithMemberById(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_COMMENT_PARENT_NOT_FOUND));

        // 대댓글에 대댓글 작성 시도 차단 (1-depth 제한)
        if (parent.isReply()) {
            throw new CustomException(ErrorCode.MATE_REPLY_NOT_ALLOWED);
        }

        // 다른 게시글의 댓글에 대댓글 작성 시도 차단
        if (!parent.getMatePost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.MATE_REPLY_POST_MISMATCH);
        }

        // 삭제된 댓글에 대댓글 작성 시도 차단
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.MATE_REPLY_TO_DELETED_COMMENT);
        }

        MateComment reply = MateComment.builder()
                .matePost(post)
                .member(author)
                .parent(parent)
                .content(req.getContent())
                .build();

        commentRepository.save(reply);
        return MateReplyResponse.of(reply);
    }

    /* ===== 수정 ===== */

    @Transactional
    public MateCommentResponse update(Long accountId, Long postId, Long commentId,
            MateCommentUpdateRequest req) {
        Member author = resolveMember(accountId);
        getActivePost(postId);

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
        getActivePost(postId);

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
