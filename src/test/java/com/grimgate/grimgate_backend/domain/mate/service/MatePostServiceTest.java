package com.grimgate.grimgate_backend.domain.mate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostStatsResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.entity.ExperienceLevel;
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
 * 메이트 모집글 서비스 단위 테스트.
 *
 * <ul>
 *   <li>인증된 사용자의 accountId 를 입력 받아 Member 로 변환하는 흐름 검증</li>
 *   <li>도메인 규칙(meeting_time / deadline) 검증</li>
 *   <li>작성자 본인이 아닐 경우 수정/삭제 거절</li>
 *   <li>통계 집계 동작</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MatePostServiceTest {

    @Mock private MatePostRepository matePostRepository;
    @Mock private MateParticipantRepository participantRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ThemeRepository themeRepository;

    @InjectMocks
    private MatePostService matePostService;

    private static final Long AUTHOR_ACCOUNT_ID = 100L;
    private static final Long AUTHOR_MEMBER_ID = 10L;
    private static final Long OTHER_ACCOUNT_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 20L;
    private static final Long THEME_ID = 1L;
    private static final Long POST_ID = 999L;

    private Account authorAccount;
    private Member authorMember;
    private Account otherAccount;
    private Member otherMember;
    private Theme theme;

    @BeforeEach
    void setUp() throws Exception {
        authorAccount = Account.builder().nickname("작성자").build();
        setId(authorAccount, AUTHOR_ACCOUNT_ID);
        authorMember = Member.builder().account(authorAccount).build();
        setId(authorMember, AUTHOR_MEMBER_ID);

        otherAccount = Account.builder().nickname("다른사용자").build();
        setId(otherAccount, OTHER_ACCOUNT_ID);
        otherMember = Member.builder().account(otherAccount).build();
        setId(otherMember, OTHER_MEMBER_ID);

        theme = buildTheme();
    }

    /* ===== Create ===== */

    @Test
    @DisplayName("create - meeting_time이 과거이면 MATE_POST_INVALID_MEETING_TIME 예외")
    void create_meetingTimeInPast() {
        MatePostCreateRequest req = baseCreateRequest()
                .meetingTime(LocalDateTime.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> matePostService.create(AUTHOR_ACCOUNT_ID, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_INVALID_MEETING_TIME.getMessage());
    }

    @Test
    @DisplayName("create - deadline이 meeting_time보다 늦으면 MATE_POST_INVALID_DEADLINE 예외")
    void create_deadlineAfterMeeting() {
        LocalDateTime meeting = LocalDateTime.now().plusDays(3);
        MatePostCreateRequest req = baseCreateRequest()
                .meetingTime(meeting)
                .deadline(meeting.plusHours(1))
                .build();

        assertThatThrownBy(() -> matePostService.create(AUTHOR_ACCOUNT_ID, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_INVALID_DEADLINE.getMessage());
    }

    @Test
    @DisplayName("create - accountId로 Member를 찾지 못하면 MEMBER_NOT_FOUND 예외")
    void create_memberNotFound() {
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.empty());

        MatePostCreateRequest req = baseCreateRequest().build();
        assertThatThrownBy(() -> matePostService.create(AUTHOR_ACCOUNT_ID, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("create - 정상 생성: 작성자 자동 1명 포함, RECRUITING 상태, openChatUrl 노출")
    void create_success() {
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));
        when(themeRepository.findById(THEME_ID)).thenReturn(Optional.of(theme));
        when(matePostRepository.save(any(MatePost.class))).thenAnswer(inv -> {
            MatePost saved = inv.getArgument(0);
            setId(saved, POST_ID);
            return saved;
        });

        MatePostCreateRequest req = baseCreateRequest().build();
        MatePostResponse res = matePostService.create(AUTHOR_ACCOUNT_ID, req);

        assertThat(res.getId()).isEqualTo(POST_ID);
        assertThat(res.getMemberId()).isEqualTo(AUTHOR_MEMBER_ID);
        assertThat(res.getCurrentPeople()).isEqualTo(1);
        assertThat(res.getMaxPeople()).isEqualTo(4);
        assertThat(res.getStatus()).isEqualTo(MatePostStatus.RECRUITING);
        // 작성자에게는 openChatUrl 노출
        assertThat(res.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/abc123");
        assertThat(res.getAuthorNickname()).isEqualTo("작성자");
        assertThat(res.getTags()).containsExactly("힐링", "초보환영");
    }

    /* ===== Update ===== */

    @Test
    @DisplayName("update - 작성자가 아니면 MATE_POST_FORBIDDEN 예외")
    void update_forbidden() throws Exception {
        MatePost post = buildPostByAuthor();
        when(memberRepository.findByAccount_Id(OTHER_ACCOUNT_ID)).thenReturn(Optional.of(otherMember));
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        MatePostUpdateRequest req = MatePostUpdateRequest.builder()
                .title("바뀐 제목입니다")
                .build();

        assertThatThrownBy(() -> matePostService.update(OTHER_ACCOUNT_ID, POST_ID, req))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("update - 작성자 본인이면 부분 수정 성공 (null 필드는 무시)")
    void update_success_partial() throws Exception {
        MatePost post = buildPostByAuthor();
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        MatePostUpdateRequest req = MatePostUpdateRequest.builder()
                .title("수정된 제목입니다")
                .build();

        MatePostResponse res = matePostService.update(AUTHOR_ACCOUNT_ID, POST_ID, req);

        assertThat(res.getTitle()).isEqualTo("수정된 제목입니다");
        // 수정되지 않은 필드는 유지
        assertThat(res.getCurrentPeople()).isEqualTo(1);
        assertThat(res.getMaxPeople()).isEqualTo(4);
    }

    /* ===== Detail ===== */

    @Test
    @DisplayName("getDetail - 비로그인 사용자에게는 openChatUrl 미노출")
    void detail_anonymous_hidesOpenChatUrl() throws Exception {
        MatePost post = buildPostByAuthor();
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        MatePostResponse res = matePostService.getDetail(POST_ID, null);
        assertThat(res.getOpenChatUrl()).isNull();
    }

    @Test
    @DisplayName("getDetail - 작성자 본인에게는 openChatUrl 노출")
    void detail_author_seesOpenChatUrl() throws Exception {
        MatePost post = buildPostByAuthor();
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));

        MatePostResponse res = matePostService.getDetail(POST_ID, AUTHOR_ACCOUNT_ID);
        assertThat(res.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/abc123");
    }

    @Test
    @DisplayName("getDetail - soft delete된 글은 MATE_POST_NOT_FOUND")
    void detail_softDeleted() throws Exception {
        MatePost post = buildPostByAuthor();
        post.softDelete();
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> matePostService.getDetail(POST_ID, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_NOT_FOUND.getMessage());
    }

    /* ===== Delete ===== */

    @Test
    @DisplayName("softDelete - 작성자 본인이면 status=DELETED, deletedAt 설정")
    void softDelete_success() throws Exception {
        MatePost post = buildPostByAuthor();
        when(memberRepository.findByAccount_Id(AUTHOR_ACCOUNT_ID)).thenReturn(Optional.of(authorMember));
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        matePostService.softDelete(AUTHOR_ACCOUNT_ID, POST_ID);

        assertThat(post.getStatus()).isEqualTo(MatePostStatus.DELETED);
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("softDelete - 작성자가 아니면 MATE_POST_FORBIDDEN")
    void softDelete_forbidden() throws Exception {
        MatePost post = buildPostByAuthor();
        when(memberRepository.findByAccount_Id(OTHER_ACCOUNT_ID)).thenReturn(Optional.of(otherMember));
        when(matePostRepository.findDetailById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> matePostService.softDelete(OTHER_ACCOUNT_ID, POST_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MATE_POST_FORBIDDEN.getMessage());
    }

    /* ===== Stats ===== */

    @Test
    @DisplayName("stats - 오늘 신규 / 모집중(RECRUITING+CLOSING_SOON) / 매칭완료 카운트 집계")
    void stats() {
        lenient().when(matePostRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(any()))
                .thenReturn(3L);
        when(matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.RECRUITING)).thenReturn(5L);
        when(matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.CLOSING_SOON)).thenReturn(2L);
        when(matePostRepository.countByStatusAndDeletedAtIsNull(MatePostStatus.MATCHED)).thenReturn(7L);

        MatePostStatsResponse res = matePostService.stats();

        assertThat(res.getTodayNewCount()).isEqualTo(3L);
        assertThat(res.getRecruitingCount()).isEqualTo(7L);    // 5 + 2
        assertThat(res.getTotalMatchedCount()).isEqualTo(7L);
    }

    /* ===== Helpers ===== */

    private MatePostCreateRequest.MatePostCreateRequestBuilder baseCreateRequest() {
        return MatePostCreateRequest.builder()
                .themeId(THEME_ID)
                .title("저녁에 같이 방탈출 가실 분")
                .content("초보 환영합니다")
                .meetingTime(LocalDateTime.now().plusDays(2))
                .deadline(LocalDateTime.now().plusDays(1))
                .maxPeople(4)
                .tags(List.of("힐링", "초보환영"))
                .experienceLevel(ExperienceLevel.BEGINNER)
                .openChatUrl("https://open.kakao.com/o/abc123")
                .imageUrl(null);
    }

    private MatePost buildPostByAuthor() throws Exception {
        MatePost post = MatePost.builder()
                .member(authorMember)
                .theme(theme)
                .title("저녁에 같이 방탈출")
                .content("초보 환영")
                .meetingTime(LocalDateTime.now().plusDays(2))
                .deadline(LocalDateTime.now().plusDays(1))
                .currentPeople(1)
                .maxPeople(4)
                .openChatUrl("https://open.kakao.com/o/abc123")
                .status(MatePostStatus.RECRUITING)
                .build();
        setId(post, POST_ID);
        return post;
    }

    private Theme buildTheme() {
        return Theme.builder()
                .title("공포의 정원")
                .description("desc")
                .build();
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
