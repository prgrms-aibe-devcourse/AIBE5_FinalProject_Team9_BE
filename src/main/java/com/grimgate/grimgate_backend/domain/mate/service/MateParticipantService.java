package com.grimgate.grimgate_backend.domain.mate.service;

import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantResponse;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipant;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipantStatus;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.repository.MateParticipantRepository;
import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메이트 모집글 참여 서비스.
 *
 * <p>도메인 규칙</p>
 * <ul>
 *   <li>작성자는 자신의 모집글에 참여할 수 없다 (작성자는 currentPeople 에 이미 +1 되어 있음)</li>
 *   <li>참여는 status 가 RECRUITING / CLOSING_SOON 일 때만 허용</li>
 *   <li>이미 JOINED 상태인 회원은 중복 참여 불가</li>
 *   <li>취소/강퇴된 row 가 있다면 동일 row 의 status 를 다시 JOINED 로 되돌려 재참여 처리</li>
 *   <li>참여/취소/강퇴는 MatePost 의 currentPeople 과 자동 동기화</li>
 *   <li>강퇴는 작성자만 가능 (자기 자신 강퇴는 불가)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MateParticipantService {

    private final MateParticipantRepository participantRepository;
    private final MatePostRepository matePostRepository;
    private final MemberRepository memberRepository;

    /* ===== Command ===== */

    /** 참여 신청 */
    @Transactional
    public MateParticipantResponse join(Long accountId, Long matePostId) {
        MatePost post = findActivePostForUpdate(matePostId);
        Member member = resolveMember(accountId);

        if (post.isAuthor(member.getId())) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_AUTHOR_CANNOT_JOIN);
        }
        if (!post.isRecruitable()) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_NOT_RECRUITING);
        }
        if (post.getDeadline() != null && post.getDeadline().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_DEADLINE_PASSED);
        }
        if (post.getCurrentPeople() >= post.getMaxPeople()) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_FULL);
        }

        MateParticipant participant = participantRepository
                .findByMatePost_IdAndMember_Id(post.getId(), member.getId())
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new CustomException(ErrorCode.MATE_PARTICIPANT_ALREADY_JOINED);
                    }
                    existing.rejoin();
                    return existing;
                })
                .orElseGet(() -> participantRepository.save(MateParticipant.join(post, member)));

        post.increaseParticipant();
        return MateParticipantResponse.fromWithOpenChat(participant);
    }

    /** 본인 참여 취소 (자기가 나감) */
    @Transactional
    public MateParticipantResponse cancel(Long accountId, Long matePostId) {
        MatePost post = findActivePostForUpdate(matePostId);
        Member member = resolveMember(accountId);

        // 명세 MP-002: RECRUITING / CLOSING_SOON 상태에서만 취소 허용 (MATCHED/CLOSED 후에는 취소 불가)
        if (!post.isRecruitable()) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_CANCEL_NOT_ALLOWED);
        }

        MateParticipant participant = participantRepository
                .findByMatePost_IdAndMember_Id(post.getId(), member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_PARTICIPANT_NOT_FOUND));

        if (!participant.isActive()) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_NOT_JOINED);
        }

        participant.cancel();
        post.decreaseParticipant();
        return MateParticipantResponse.from(participant);
    }

    /** 작성자가 참여자 강퇴 */
    @Transactional
    public void kick(Long accountId, Long matePostId, Long targetMemberId) {
        MatePost post = findActivePostForUpdate(matePostId);
        Member author = resolveMember(accountId);

        if (!post.isAuthor(author.getId())) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_KICK_FORBIDDEN);
        }
        if (author.getId().equals(targetMemberId)) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_KICK_SELF);
        }

        MateParticipant participant = participantRepository
                .findByMatePost_IdAndMember_Id(post.getId(), targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_PARTICIPANT_NOT_FOUND));

        if (!participant.isActive()) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_NOT_JOINED);
        }

        participant.kick();
        post.decreaseParticipant();
    }

    /* ===== Query ===== */

    /**
     * 모집글 참여자 목록 조회 (활성만).
     * 명세 MP-003: 작성자만 조회 가능.
     */
    public MateParticipantListResponse listParticipants(Long accountId, Long matePostId) {
        MatePost post = findActivePost(matePostId);
        Member viewer = resolveMember(accountId);

        if (!post.isAuthor(viewer.getId())) {
            throw new CustomException(ErrorCode.MATE_PARTICIPANT_LIST_FORBIDDEN);
        }

        List<MateParticipantResponse> items = participantRepository
                .findActiveByMatePostId(post.getId(), MateParticipantStatus.JOINED)
                .stream()
                .map(MateParticipantResponse::from)
                .toList();

        return MateParticipantListResponse.builder()
                .matePostId(post.getId())
                .currentPeople(post.getCurrentPeople())
                .maxPeople(post.getMaxPeople())
                .items(items)
                .build();
    }

    /** 내 참여 목록 (마이페이지 등) */
    public List<MateParticipantResponse> myParticipations(Long accountId) {
        Member me = resolveMember(accountId);
        return participantRepository
                .findMyParticipations(me.getId(), MateParticipantStatus.JOINED)
                .stream()
                .map(MateParticipantResponse::from)
                .toList();
    }

    /* ===== Helpers ===== */

    private MatePost findActivePost(Long matePostId) {
        MatePost post = matePostRepository.findById(matePostId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATE_POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new CustomException(ErrorCode.MATE_POST_NOT_FOUND);
        }
        return post;
    }

    /**
     * 동시 참여/취소/강퇴 시 currentPeople 경합 방지용 비관적 쓰기 락 버전.
     */
    private MatePost findActivePostForUpdate(Long matePostId) {
        MatePost post = matePostRepository.findByIdForUpdate(matePostId)
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
}
