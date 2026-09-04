package com.grimgate.grimgate_backend.domain.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.time.Duration;
import java.util.List;
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

/**
 * Mockito를 기반으로 SlotHoldService 비즈니스 로직을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class SlotHoldServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SlotHoldService slotHoldService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Member setupSecurityContextAndMember(Long accountId, Long memberId) {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);

        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);
        Mockito.when(userDetails.getUsername()).thenReturn(String.valueOf(accountId));

        SecurityContextHolder.setContext(securityContext);

        Member member = Member.builder()
                .id(memberId)
                .build();

        Mockito.when(memberRepository.findByAccount_Id(accountId))
                .thenReturn(Optional.of(member));

        return member;
    }

    @Test
    @DisplayName("HOLD 성공 - 슬롯이 존재하고 AVAILABLE 상태이며 Redis 선점에 성공한 경우 holdToken과 300초 만료 시간을 반환해야 한다")
    void holdSlot_Success() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("hold:slot:" + timeSlotId), anyString(), eq(Duration.ofMinutes(5))))
                .thenReturn(true);

        // when
        SlotHoldResponse response = slotHoldService.holdSlot(timeSlotId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTimeSlotId()).isEqualTo(timeSlotId);
        assertThat(response.getHoldToken()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(300L);

        verify(timeSlotRepository).findById(timeSlotId);
        verify(valueOperations).setIfAbsent(eq("hold:slot:" + timeSlotId), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("HOLD 실패 - 존재하지 않는 타임슬롯을 조회할 경우 404 NOT_FOUND 예외를 던져야 한다")
    void holdSlot_TimeSlotNotFound() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);

        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> slotHoldService.holdSlot(timeSlotId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(responseStatusEx.getReason()).isEqualTo("존재하지 않는 슬롯입니다.");
                });
    }

    @Test
    @DisplayName("HOLD 실패 - 타임슬롯 상태가 SLOT_AVAILABLE이 아닐 경우 409 CONFLICT 예외를 던져야 한다")
    void holdSlot_NotAvailableStatus() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));

        // when & then
        assertThatThrownBy(() -> slotHoldService.holdSlot(timeSlotId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusEx.getReason()).isEqualTo("예약 가능한 슬롯 상태가 아닙니다.");
                });
    }

    @Test
    @DisplayName("HOLD 실패 - Redis setIfAbsent가 false를 반환하여 선점에 실패할 경우 409 CONFLICT 예외를 던져야 한다")
    void holdSlot_RedisSetIfAbsentFalse() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("hold:slot:" + timeSlotId), anyString(), eq(Duration.ofMinutes(5))))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> slotHoldService.holdSlot(timeSlotId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusEx.getReason()).isEqualTo("이미 다른 사용자가 선점 중인 슬롯입니다.");
                });
    }

    @Test
    @DisplayName("HOLD 실패 - Redis setIfAbsent가 null을 반환하여 선점에 실패할 경우 409 CONFLICT 예외를 던져야 한다")
    void holdSlot_RedisSetIfAbsentNull() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("hold:slot:" + timeSlotId), anyString(), eq(Duration.ofMinutes(5))))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> slotHoldService.holdSlot(timeSlotId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusEx.getReason()).isEqualTo("이미 다른 사용자가 선점 중인 슬롯입니다.");
                });
    }

    @Test
    @DisplayName("HOLD 해제 성공 - Redis에 저장된 선점 정보와 일치할 경우 삭제하고 released=true를 반환해야 한다")
    void releaseSlot_Success() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);
        String holdToken = "some-token";
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken(holdToken)
                .build();

        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(1L);

        // when
        SlotReleaseResponse response = slotHoldService.releaseSlot(timeSlotId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTimeSlotId()).isEqualTo(timeSlotId);
        assertThat(response.isReleased()).isTrue();

        verify(stringRedisTemplate).execute(any(RedisScript.class), any(List.class), any());
    }

    @Test
    @DisplayName("HOLD 해제 실패 - Redis에 선점 정보가 존재하지 않을 경우(Lua script -1 반환) 404 NOT_FOUND 예외를 던져야 한다")
    void releaseSlot_NotFound() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken("some-token")
                .build();

        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(-1L);

        // when & then
        assertThatThrownBy(() -> slotHoldService.releaseSlot(timeSlotId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(responseStatusEx.getReason()).isEqualTo("선점 정보가 존재하지 않습니다.");
                });
    }

    @Test
    @DisplayName("HOLD 해제 실패 - Redis에 저장된 값과 요청값이 일치하지 않을 경우(Lua script -2 반환) 409 CONFLICT 예외를 던져야 한다")
    void releaseSlot_Conflict() {
        // given
        Long timeSlotId = 1L;
        setupSecurityContextAndMember(200L, 100L);
        SlotReleaseRequest request = SlotReleaseRequest.builder()
                .holdToken("some-token")
                .build();

        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any()))
                .thenReturn(-2L);

        // when & then
        assertThatThrownBy(() -> slotHoldService.releaseSlot(timeSlotId, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusEx = (ResponseStatusException) ex;
                    assertThat(responseStatusEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseStatusEx.getReason()).isEqualTo("선점 정보가 일치하지 않습니다.");
                });
    }
}
