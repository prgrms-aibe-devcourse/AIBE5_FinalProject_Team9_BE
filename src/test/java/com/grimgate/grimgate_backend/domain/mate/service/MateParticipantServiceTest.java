package com.grimgate.grimgate_backend.domain.mate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantResponse;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipant;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipantStatus;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePostStatus;
import com.grimgate.grimgate_backend.domain.mate.repository.MateParticipantRepository;
import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 메이트 참여(MateParticipant) 서비스 단위 테스트.
 *
 * <p>도메인 규칙 검증</p>
 * <ul>
 *   <li>작성자 본인 참여 차단</li>
 *   <li>모집 상태 / 정원 검증</li>
 *   <li>중복 참여 차단 + 취소된 row 의 rejoin 동작</li>
 *   <li>본인 취소 / 작성자 강퇴(자기 자신 강퇴 차단 포함)</li>
 *   <li>currentPeople 동기화 (increase/decrease 호출 여부)</li>
 *   <li>참여자 목록 / 내 참여 목록 조회</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MateParticipantServiceTest {

    @Mock private MateParticipantRepository participantRepository;
    @Mock private MatePostRepository matePostRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private MateParticipantService participantService;

    private static final Long AUTHOR_ACCOUNT_ID = 100L;
    private static final Long AUTHOR_MEMBER_ID = 10L;
    private static final Long GUEST_ACCOUNT_ID = 200L;
    private static final Long GUEST_MEMBER_ID = 20L;
    private static final Long POST_ID = 999L;

    private Account authorAccount;
    private Member authorMember;
    private Account guestAccount;
    private Member guestMember;
    private Theme theme;

    @BeforeEach
    void setUp() throws Exception {
        authorAccount = Account.builder().nickname("작성자").build();
        setId(authorAccount, AUTHOR_ACCOUNT_ID);
        authorMember = Member.builder().account(authorAccount).build();
        setId(authorMember, AUTHOR_MEMBER_ID);

        guestAccount = Account.builder().nickname("게스트").build();
        setId(guestAccount, GUEST_ACCOUNT_ID);
        guestMember = Member.builder().account(guestAccount).build();
        setId(guestMember, GUEST_MEMBER_ID);

        theme = Theme.builder().title("공포의 정원").description("desc").build();
    }

    /* ===== join ===== */

    @Test
    @DisplayName("join - 모집글이 없으면 MATE_POST_NOT_FOUND")
    void join_postNotFound() {
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.join(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("join - 작성자 본인이 참여 시도하면 AUTHOR_CANNOT_JOIN")
    void join_authorCannotJoin() throws Exception {
        MatePost post = buildPost(1, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));

        assertThatThrownBy(() -> participantService.join(AUTHOR_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_AUTHOR_CANNOT_JOIN.getMessage());
    }

    @Test
    @DisplayName("join - 모집중 상태가 아니면 NOT_RECRUITING")
    void join_notRecruiting() throws Exception {
        MatePost post = buildPost(4, 4, MatePostStatus.MATCHED);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.join(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_NOT_RECRUITING.getMessage());
    }

    @Test
    @DisplayName("join - 정원이 가득 차면 FULL")
    void join_full() throws Exception {
        MatePost post = buildPost(4, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.join(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_FULL.getMessage());
    }

    @Test
    @DisplayName("join - 마감(deadline)이 지난 모집글이면 DEADLINE_PASSED")
    void join_deadlinePassed() throws Exception {
        MatePost post = buildPostWithDeadline(2, 4, MatePostStatus.RECRUITING,
                LocalDateTime.now().minusHours(1));
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.join(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_DEADLINE_PASSED.getMessage());
    }

    @Test
    @DisplayName("join - 이미 JOINED 상태면 ALREADY_JOINED")
    void join_alreadyJoined() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        MateParticipant existing = MateParticipant.join(post, guestMember);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> participantService.join(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_ALREADY_JOINED.getMessage());
    }

    @Test
    @DisplayName("join - 최초 참여: save 호출 + currentPeople +1")
    void join_firstSuccess() throws Exception {
        MatePost post = buildPost(1, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(MateParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        MateParticipantResponse res = participantService.join(GUEST_ACCOUNT_ID, POST_ID);

        assertThat(res.getMemberId()).isEqualTo(GUEST_MEMBER_ID);
        assertThat(res.getStatus()).isEqualTo(MateParticipantStatus.JOINED);
        assertThat(post.getCurrentPeople()).isEqualTo(2);
        verify(participantRepository).save(any(MateParticipant.class));
    }

    @Test
    @DisplayName("join - 취소된 row 가 있으면 rejoin: save 호출 안 함(dirty checking) + currentPeople +1")
    void join_rejoinDirtyChecking() throws Exception {
        MatePost post = buildPost(1, 4, MatePostStatus.RECRUITING);
        MateParticipant cancelled = MateParticipant.join(post, guestMember);
        cancelled.cancel();
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.of(cancelled));

        MateParticipantResponse res = participantService.join(GUEST_ACCOUNT_ID, POST_ID);

        assertThat(res.getStatus()).isEqualTo(MateParticipantStatus.JOINED);
        assertThat(cancelled.getStatus()).isEqualTo(MateParticipantStatus.JOINED);
        assertThat(cancelled.getCancelledAt()).isNull();
        assertThat(post.getCurrentPeople()).isEqualTo(2);
        // rejoin 은 새 INSERT 가 아니라 같은 row 의 dirty checking 으로 UPDATE 되어야 함
        verify(participantRepository, never()).save(any(MateParticipant.class));
    }

    /* ===== cancel ===== */

    @Test
    @DisplayName("cancel - 참여 정보가 없으면 NOT_FOUND")
    void cancel_notFound() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> participantService.cancel(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("cancel - 이미 취소/강퇴된 상태면 NOT_JOINED")
    void cancel_alreadyInactive() throws Exception {
        MatePost post = buildPost(1, 4, MatePostStatus.RECRUITING);
        MateParticipant cancelled = MateParticipant.join(post, guestMember);
        cancelled.cancel();
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> participantService.cancel(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_NOT_JOINED.getMessage());
    }

    @Test
    @DisplayName("cancel - 정상 취소: status=CANCELLED, currentPeople -1, DTO 반환")
    void cancel_success() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        MateParticipant active = MateParticipant.join(post, guestMember);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.of(active));

        MateParticipantResponse res = participantService.cancel(GUEST_ACCOUNT_ID, POST_ID);

        assertThat(active.getStatus()).isEqualTo(MateParticipantStatus.CANCELLED);
        assertThat(active.getCancelledAt()).isNotNull();
        assertThat(post.getCurrentPeople()).isEqualTo(1);
        assertThat(res.getStatus()).isEqualTo(MateParticipantStatus.CANCELLED);
        assertThat(res.getMemberId()).isEqualTo(GUEST_MEMBER_ID);
    }

    @Test
    @DisplayName("cancel - 모집글이 MATCHED 상태면 CANCEL_NOT_ALLOWED")
    void cancel_notAllowedWhenMatched() throws Exception {
        MatePost post = buildPost(4, 4, MatePostStatus.MATCHED);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.cancel(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_CANCEL_NOT_ALLOWED.getMessage());
    }

    /* ===== kick ===== */

    @Test
    @DisplayName("kick - 작성자가 아니면 KICK_FORBIDDEN")
    void kick_forbidden() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        // 비작성자(게스트)가 강퇴 시도
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.kick(GUEST_ACCOUNT_ID, POST_ID, AUTHOR_MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_KICK_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("kick - 작성자 본인을 강퇴 시도하면 KICK_SELF")
    void kick_self() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));

        assertThatThrownBy(() -> participantService.kick(AUTHOR_ACCOUNT_ID, POST_ID, AUTHOR_MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_KICK_SELF.getMessage());
    }

    @Test
    @DisplayName("kick - 정상 강퇴: status=KICKED, currentPeople -1")
    void kick_success() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        MateParticipant joined = MateParticipant.join(post, guestMember);
        when(matePostRepository.findByIdForUpdate(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));
        when(participantRepository.findByMatePost_IdAndMember_Id(POST_ID, GUEST_MEMBER_ID))
                .thenReturn(Optional.of(joined));

        participantService.kick(AUTHOR_ACCOUNT_ID, POST_ID, GUEST_MEMBER_ID);

        assertThat(joined.getStatus()).isEqualTo(MateParticipantStatus.KICKED);
        assertThat(post.getCurrentPeople()).isEqualTo(1);
    }

    /* ===== queries ===== */

    @Test
    @DisplayName("listParticipants - 작성자가 조회: 활성 참여자만 응답에 포함")
    void listParticipants_authorOk() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        MateParticipant joined = MateParticipant.join(post, guestMember);
        when(matePostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));
        when(participantRepository.findActiveByMatePostId(POST_ID, MateParticipantStatus.JOINED))
                .thenReturn(List.of(joined));

        MateParticipantListResponse res = participantService.listParticipants(AUTHOR_ACCOUNT_ID, POST_ID);

        assertThat(res.getMatePostId()).isEqualTo(POST_ID);
        assertThat(res.getCurrentPeople()).isEqualTo(2);
        assertThat(res.getMaxPeople()).isEqualTo(4);
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getMemberId()).isEqualTo(GUEST_MEMBER_ID);
    }

    @Test
    @DisplayName("listParticipants - 비작성자가 조회하면 LIST_FORBIDDEN")
    void listParticipants_nonAuthorForbidden() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        when(matePostRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));

        assertThatThrownBy(() -> participantService.listParticipants(GUEST_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_PARTICIPANT_LIST_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("myParticipations - 내가 참여중인 모집글 목록 조회")
    void myParticipations() throws Exception {
        MatePost post = buildPost(2, 4, MatePostStatus.RECRUITING);
        MateParticipant joined = MateParticipant.join(post, guestMember);
        when(memberRepository.findByAccount_Id(GUEST_ACCOUNT_ID)).thenReturn(Optional.of(guestMember));
        when(participantRepository.findMyParticipations(GUEST_MEMBER_ID, MateParticipantStatus.JOINED))
                .thenReturn(List.of(joined));

        List<MateParticipantResponse> res = participantService.myParticipations(GUEST_ACCOUNT_ID);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getMatePostId()).isEqualTo(POST_ID);
        assertThat(res.get(0).getStatus()).isEqualTo(MateParticipantStatus.JOINED);
    }

    /* ===== Helpers ===== */

    private MatePost buildPost(int currentPeople, int maxPeople, MatePostStatus status) throws Exception {
        return buildPostWithDeadline(currentPeople, maxPeople, status,
                LocalDateTime.now().plusDays(1));
    }

    private MatePost buildPostWithDeadline(int currentPeople, int maxPeople,
                                            MatePostStatus status,
                                            LocalDateTime deadline) throws Exception {
        MatePost post = MatePost.builder()
                .member(authorMember)
                .theme(theme)
                .title("저녁에 같이 방탈출")
                .content("초보 환영")
                .meetingTime(LocalDateTime.now().plusDays(2))
                .deadline(deadline)
                .currentPeople(currentPeople)
                .maxPeople(maxPeople)
                .openChatUrl("https://open.kakao.com/o/abc123")
                .status(status)
                .build();
        setId(post, POST_ID);
        return post;
    }

    private static void setId(Object target, Long id) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField("id");
                f.setAccessible(true);
                f.set(target, id);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }
}
