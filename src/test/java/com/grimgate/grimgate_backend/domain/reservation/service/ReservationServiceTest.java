package com.grimgate.grimgate_backend.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCancelResponse;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateRequest;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateResponse;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Member setupSecurityContextAndMember(Long accountId, Member member) {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);

        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);
        Mockito.when(userDetails.getUsername()).thenReturn(String.valueOf(accountId));

        SecurityContextHolder.setContext(securityContext);

        Mockito.when(memberRepository.findByAccount_Id(accountId))
                .thenReturn(Optional.of(member));

        return member;
    }

    @Test
    @DisplayName("예약 생성 성공 - 모든 검증을 통과하고 예약이 PENDING_PAYMENT 상태로 저장된다")
    void createReservation_Success() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        int peopleCount = 3;

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(peopleCount)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        Member member = Member.builder()
                .id(memberId)
                .build();
        setupSecurityContextAndMember(200L, member);

        Reservation savedReservation = Reservation.builder()
                .id(50L)
                .timeSlot(timeSlot)
                .member(member)
                .peopleCount(peopleCount)
                .totalPrice(66000)
                .status(ReservationStatus.PENDING_PAYMENT)
                .termsAgreedAt(LocalDateTime.now())
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_HELD), eq(TimeSlotStatus.SLOT_AVAILABLE), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        // when
        ReservationCreateResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(50L);
        assertThat(response.getTimeSlotId()).isEqualTo(timeSlotId);
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(response.getPeopleCount()).isEqualTo(peopleCount);
        assertThat(response.getTotalPrice()).isEqualTo(66000);

        verify(timeSlotRepository).updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_HELD), eq(TimeSlotStatus.SLOT_AVAILABLE), any(LocalDateTime.class));
        verify(reservationRepository).save(any(Reservation.class));
        verify(stringRedisTemplate).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("예약 생성 실패 - Redis 선점 정보가 존재하지 않아 404 NOT_FOUND 발생")
    void createReservation_RedisHoldNotFound() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        setupSecurityContextAndMember(200L, Member.builder().id(memberId).build());

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("선점 정보가 존재하지 않습니다.");
                });

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 생성 실패 - Redis 선점 정보(토큰)가 불일치하여 409 CONFLICT 발생")
    void createReservation_RedisHoldMismatch() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":different-token";
        setupSecurityContextAndMember(200L, Member.builder().id(memberId).build());

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusEx.getReason()).isEqualTo("선점 정보가 일치하지 않습니다.");
                });

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 생성 실패 - 회원 정보가 존재하지 않아 404 NOT_FOUND 발생")
    void createReservation_MemberNotFound() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        // SecurityContext 직접 모킹 (Member empty 반환)
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("200");
        SecurityContextHolder.setContext(securityContext);

        when(memberRepository.findByAccount_Id(200L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("회원을 찾을 수 없습니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 타임슬롯 정보가 존재하지 않아 404 NOT_FOUND 발생")
    void createReservation_TimeSlotNotFound() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("존재하지 않는 슬롯입니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 타임슬롯의 상태가 SLOT_AVAILABLE이 아니라서 409 CONFLICT 발생")
    void createReservation_TimeSlotNotAvailable() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusEx.getReason()).isEqualTo("예약 가능한 슬롯 상태가 아닙니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 인원 수가 테마의 예약 가능 범위를 벗어나 400 BAD_REQUEST 발생")
    void createReservation_PeopleCountOutOfRange() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(6) // max가 5인 상황에서 6명 요청
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("인원 수 범위를 초과했습니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 슬롯 상태를 HELD로 변경하는 과정에서 경쟁으로 인해 업데이트 결과가 0이라서 409 CONFLICT 발생")
    void createReservation_TimeSlotUpdateConflict() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_HELD), eq(TimeSlotStatus.SLOT_AVAILABLE), any(LocalDateTime.class)))
                .thenReturn(0); // 0개 행 업데이트됨

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusEx.getReason()).isEqualTo("이미 다른 사용자가 선점 중이거나 예약이 완료된 슬롯입니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 약관 동의(termsAgreed)가 false인 경우 400 Bad Request 발생")
    void createReservation_TermsAgreedFalse() {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(10L)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(false)
                .build();

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("서비스 이용약관에 동의해야 합니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 약관 동의(termsAgreed)가 null인 경우 400 Bad Request 발생")
    void createReservation_TermsAgreedNull() {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(10L)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(null)
                .build();

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("서비스 이용약관에 동의해야 합니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 연령 제한이 있으나 회원의 나이 정보가 null인 경우 400 Bad Request 발생")
    void createReservation_AccountAgeNull() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .ageLimit(15) // 연령 제한 15세
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        com.grimgate.grimgate_backend.domain.account.entity.Account account = 
                com.grimgate.grimgate_backend.domain.account.entity.Account.builder()
                        .age(null) // 나이 정보 null
                        .build();

        Member member = Member.builder()
                .id(memberId)
                .account(account)
                .build();
        setupSecurityContextAndMember(200L, member);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("나이 정보가 필요합니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 실패 - 회원의 나이가 테마 이용 연령 제한 미달인 경우 400 Bad Request 발생")
    void createReservation_UnderAgeLimit() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .ageLimit(19) // 연령 제한 19세
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        com.grimgate.grimgate_backend.domain.account.entity.Account account = 
                com.grimgate.grimgate_backend.domain.account.entity.Account.builder()
                        .age(17) // 17세 (제한 미달)
                        .build();

        Member member = Member.builder()
                .id(memberId)
                .account(account)
                .build();
        setupSecurityContextAndMember(200L, member);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("테마 이용 연령 제한 미달입니다.");
                });
    }

    @Test
    @DisplayName("예약 생성 성공 - 테마 연령 제한이 0 또는 null인 경우 연령 검증을 통과하고 성공한다")
    void createReservation_NoAgeLimit_Success() {
        // given
        Long memberId = 1L;
        Long timeSlotId = 10L;
        String holdToken = "hold-token-123";
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        String redisKey = "hold:slot:" + timeSlotId;
        String redisValue = memberId + ":" + holdToken;

        Theme theme = Theme.builder()
                .id(100L)
                .ageLimit(0) // 연령 제한 없음 (0)
                .minPeople(2)
                .maxPeople(5)
                .price(22000)
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .theme(theme)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        Member member = Member.builder()
                .id(memberId)
                .build(); // account가 null이어도 통과해야 함
        setupSecurityContextAndMember(200L, member);

        Reservation savedReservation = Reservation.builder()
                .id(50L)
                .timeSlot(timeSlot)
                .member(member)
                .peopleCount(3)
                .totalPrice(66000)
                .status(ReservationStatus.PENDING_PAYMENT)
                .termsAgreedAt(LocalDateTime.now())
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(redisValue);
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_HELD), eq(TimeSlotStatus.SLOT_AVAILABLE), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        // when
        ReservationCreateResponse response = reservationService.createReservation(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    @DisplayName("예약 취소 성공 - PENDING_PAYMENT 상태이며 결제 정보가 존재할 때 결제 실패로 변경 및 슬롯 복구")
    void cancelReservation_Success_PendingPayment_WithPayment() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;
        Long timeSlotId = 10L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .timeSlot(timeSlot)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .reservation(reservation)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when
        ReservationCancelResponse response = reservationService.cancelReservation(reservationId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getPaymentId()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAY_FAILED);
        assertThat(payment.getCancelReason()).isEqualTo("사용자 예약 취소로 결제 진행 중단");

        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("예약 취소 성공 - PENDING_PAYMENT 상태이며 결제 정보가 존재하지 않을 때 예약 취소 및 슬롯 복구만 진행")
    void cancelReservation_Success_PendingPayment_NoPayment() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;
        Long timeSlotId = 10L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .timeSlot(timeSlot)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when
        ReservationCancelResponse response = reservationService.cancelReservation(reservationId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");

        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("예약 취소 성공 - CONFIRMED 상태이며 결제 정보가 PAY_SUCCESS일 때 결제 환불대기로 변경 및 슬롯 복구")
    void cancelReservation_Success_Confirmed() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;
        Long timeSlotId = 10L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_FULL)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .timeSlot(timeSlot)
                .status(ReservationStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .reservation(reservation)
                .status(PaymentStatus.PAY_SUCCESS)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_FULL), any(LocalDateTime.class)))
                .thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when
        ReservationCancelResponse response = reservationService.cancelReservation(reservationId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getPaymentId()).isEqualTo(100L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAY_REFUND_PENDING);
        assertThat(payment.getCancelReason()).isEqualTo("사용자 예약 취소로 인한 환불 대기");

        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("예약 취소 실패 - CONFIRMED 상태이나 결제 정보가 존재하지 않을 때 400 Bad Request 발생")
    void cancelReservation_Confirmed_NoPayment_ThrowsException() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("결제 내역을 찾을 수 없습니다.");
                });
    }

    @Test
    @DisplayName("예약 취소 실패 - CONFIRMED 상태이나 결제 정보 상태가 PAY_SUCCESS가 아닐 때 400 Bad Request 발생")
    void cancelReservation_Confirmed_PaymentNotSuccess_ThrowsException() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .status(ReservationStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .reservation(reservation)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("결제 완료 상태의 결제 내역만 취소할 수 있습니다.");
                });
    }

    @Test
    @DisplayName("예약 취소 실패 - 타임슬롯 복구(updateStatus) 실패 시 409 Conflict 발생 및 롤백")
    void cancelReservation_TimeSlotRecoveryFailed_Rollback() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;
        Long timeSlotId = 10L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_FULL)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .timeSlot(timeSlot)
                .status(ReservationStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .reservation(reservation)
                .status(PaymentStatus.PAY_SUCCESS)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_FULL), any(LocalDateTime.class)))
                .thenReturn(0); // 복구 실패

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(statusEx.getReason()).isEqualTo("타임슬롯 상태 복구에 실패했습니다.");
                });
    }

    @Test
    @DisplayName("예약 취소 실패 - 다른 사용자의 예약을 취소하려 할 때 403 Forbidden 발생")
    void cancelReservation_Forbidden() {
        // given
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Long reservationId = 50L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        Member otherMember = Member.builder().id(otherMemberId).build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(otherMember)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(statusEx.getReason()).isEqualTo("해당 예약에 대한 권한이 없습니다.");
                });
    }

    @Test
    @DisplayName("예약 취소 실패 - 취소 불가능한 예약 상태일 때 400 Bad Request 발생")
    void cancelReservation_InvalidStatus() {
        // given
        Long memberId = 1L;
        Long reservationId = 50L;

        Member member = Member.builder().id(memberId).build();
        setupSecurityContextAndMember(200L, member);

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .status(ReservationStatus.COMPLETED) // 취소 불가능 상태
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("취소 가능한 예약 상태가 아닙니다.");
                });
    }
}
