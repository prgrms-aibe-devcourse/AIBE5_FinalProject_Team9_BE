package com.grimgate.grimgate_backend.domain.mate.service;

import com.grimgate.grimgate_backend.domain.mate.dto.MatePostCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostStatsResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePostStatus;
import com.grimgate.grimgate_backend.domain.mate.repository.MateParticipantRepository;
import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메이트 모집글 서비스.
 *
 * <p>도메인 규칙</p>
 * <ul>
 *   <li>meetingTime 은 현재 이후</li>
 *   <li>deadline ≤ meetingTime (deadline 은 선택)</li>
 *   <li>openChatUrl: {@code ^https?://open\.kakao\.com/.+} 형식 (DTO @Pattern 1차 + 서비스 2차)</li>
 *   <li>작성자 본인만 수정/삭제 가능</li>
 *   <li>openChatUrl 은 작성자 본인에게만 응답 노출 (이번 PR 범위에서는 참가자 구분 X)</li>
 * </ul>
 *
 * <p>작성자 식별</p>
 * <ul>
 *   <li>로그인 사용자의 {@code accountId} 를 입력으로 받아 {@code MemberRepository#findByAccount_Id} 로 Member 를 조회</li>
 *   <li>Controller 가 {@link com.grimgate.grimgate_backend.global.security.SecurityUtil#getCurrentAccountId()} 로 accountId 를 추출하여 전달</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatePostService {

    private final MatePostRepository matePostRepository;
    private final MateParticipantRepository participantRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;

    /* ===== Create ===== */

    @Transactional
    public MatePostResponse create(Long accountId, MatePostCreateRequest req) {
        validateMeetingTime(req.getMeetingTime());
        validateDeadline(req.getDeadline(), req.getMeetingTime());

        Member author = resolveMember(accountId);
        Theme theme = themeRepository.findById(req.getThemeId())
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));

        MatePost post = MatePost.builder()
                .member(author)
                .theme(theme)
                .title(req.getTitle())
                .content(req.getContent())
                .imageUrl(req.getImageUrl())
                .meetingTime(req.getMeetingTime())
                .deadline(req.getDeadline())
                .currentPeople(1) // 작성자 본인 포함
                .maxPeople(req.getMaxPeople())
                .tags(serializeTags(req.getTags()))
                .experienceLevel(req.getExperienceLevel())
                .openChatUrl(req.getOpenChatUrl())
                .status(MatePostStatus.RECRUITING)
                .build();

        MatePost saved = matePostRepository.save(post);
        // 작성자는 항상 openChatUrl 조회 가능
        return MatePostResponse.of(saved, true);
    }

    /* ===== Update ===== */

    @Transactional
    public MatePostResponse update(Long accountId, Long postId, MatePostUpdateRequest req) {
        Member author = resolveMember(accountId);
        MatePost post = getActivePost(postId);
        if (!post.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_POST_FORBIDDEN);
        }

        LocalDateTime newMeeting = req.getMeetingTime() != null ? req.getMeetingTime() : post.getMeetingTime();
        LocalDateTime newDeadline = req.getDeadline() != null ? req.getDeadline() : post.getDeadline();

        if (req.getMeetingTime() != null) validateMeetingTime(newMeeting);
        if (newDeadline != null) validateDeadline(newDeadline, newMeeting);

        post.update(
                req.getTitle(),
                req.getContent(),
                req.getMeetingTime(),
                req.getDeadline(),
                req.getMaxPeople(),
                serializeTags(req.getTags()),
                req.getExperienceLevel(),
                req.getOpenChatUrl(),
                req.getImageUrl()
        );
        return MatePostResponse.of(post, true);
    }

    /* ===== Detail ===== */

    public MatePostResponse getDetail(Long postId, Long currentAccountId) {
        MatePost post = getActivePost(postId);
        boolean isAuthor = false;
        if (currentAccountId != null) {
            Member current = memberRepository.findByAccount_Id(currentAccountId).orElse(null);
            isAuthor = current != null && post.isAuthor(current.getId());
        }
        // 이번 PR 에서는 작성자에게만 openChatUrl 노출
        return MatePostResponse.of(post, isAuthor);
    }

    /* ===== List ===== */

    public MatePostListResponse list(String tab,
                                     String keyword,
                                     String status,
                                     Long themeId,
                                     String experienceLevel,
                                     String sort,
                                     int page,
                                     int size,
                                     Long currentAccountId) {
        Sort sortBy = resolveSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortBy);

        MatePostStatus statusEnum = parseStatus(status);

        Long authorFilter = null;
        Long currentMemberId = null;
        if (currentAccountId != null) {
            Member current = memberRepository.findByAccount_Id(currentAccountId).orElse(null);
            if (current != null) {
                currentMemberId = current.getId();
                if ("my".equalsIgnoreCase(tab)) {
                    authorFilter = currentMemberId;
                }
            }
        }

        Specification<MatePost> spec = MatePostRepository.withFilter(
                keyword, statusEnum, themeId, experienceLevel, authorFilter);

        Page<MatePost> result = matePostRepository.findAll(spec, pageable);

        final Long finalMemberId = currentMemberId;
        Page<MatePostResponse> mapped = result.map(p -> {
            boolean isAuthor = finalMemberId != null && p.isAuthor(finalMemberId);
            return MatePostResponse.of(p, isAuthor);
        });
        return MatePostListResponse.from(mapped);
    }

    /* ===== Manual close ===== */

    @Transactional
    public void close(Long accountId, Long postId) {
        Member author = resolveMember(accountId);
        // 비관적 락으로 동시 참여 요청과 충돌 방지
        MatePost post = matePostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new CustomException(ErrorCode.MATE_POST_NOT_FOUND);
        }
        if (!post.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_POST_FORBIDDEN);
        }
        // RECRUITING 또는 CLOSING_SOON 상태일 때만 수동 마감 가능
        if (!post.isRecruitable()) {
            throw new CustomException(ErrorCode.MATE_POST_CANNOT_CLOSE);
        }
        post.close();
    }

    /* ===== Soft delete ===== */

    @Transactional
    public void softDelete(Long accountId, Long postId) {
        Member author = resolveMember(accountId);
        MatePost post = getActivePost(postId);
        if (!post.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_POST_FORBIDDEN);
        }
        // 1) 따로 모집글 먼저 soft delete — 이 시점에 dirty checking 으로 flush 해둔다
        post.softDelete();
        // 2) 활성 참여자 일괄 취소 — bulk update 가 영속성 컨텍스트를 비워도 이미 post 변경은 flush 됨
        participantRepository.cancelAllJoinedByMatePostId(post.getId(), LocalDateTime.now());
    }

    /* ===== Stats ===== */

    public MatePostStatsResponse stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNew = matePostRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(todayStart);
        long recruiting = matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.RECRUITING)
                + matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.CLOSING_SOON);
        long matched = matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.MATCHED);
        return MatePostStatsResponse.builder()
                .todayNewCount(todayNew)
                .recruitingCount(recruiting)
                .totalMatchedCount(matched)
                .build();
    }

    /* ===== Helpers ===== */

    MatePost getActivePost(Long postId) {
        // member/theme/branch 까지 함께 로딩해 응답 조립 시 추가 쿼리 발생 방지
        MatePost post = matePostRepository.findDetailById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new CustomException(ErrorCode.MATE_POST_NOT_FOUND);
        }
        return post;
    }

    private Member resolveMember(Long accountId) {
        return memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateMeetingTime(LocalDateTime meetingTime) {
        if (meetingTime == null || meetingTime.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MATE_POST_INVALID_MEETING_TIME);
        }
    }

    private void validateDeadline(LocalDateTime deadline, LocalDateTime meetingTime) {
        if (deadline == null) return;
        if (meetingTime != null && deadline.isAfter(meetingTime)) {
            throw new CustomException(ErrorCode.MATE_POST_INVALID_DEADLINE);
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",",
                tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private MatePostStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return MatePostStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank() || "latest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("deadline".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "deadline");
        }
        if ("meeting".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "meetingTime");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
