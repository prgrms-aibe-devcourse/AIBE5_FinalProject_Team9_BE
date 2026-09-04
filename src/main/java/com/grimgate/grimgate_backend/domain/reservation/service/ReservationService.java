package com.grimgate.grimgate_backend.domain.reservation.service;

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
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final RedisScript<Long> COMPARE_AND_DELETE_SCRIPT;

    static {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";
        COMPARE_AND_DELETE_SCRIPT = new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * 예약을 생성하고 PENDING_PAYMENT 상태로 저장합니다.
     *
     * @param request 예약 생성 요청 DTO
     * @return 예약 생성 결과 응답 DTO
     */
    @Transactional
    public ReservationCreateResponse createReservation(ReservationCreateRequest request) {
        // 0. 약관 동의 검증
        if (request.getTermsAgreed() == null || !request.getTermsAgreed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서비스 이용약관에 동의해야 합니다.");
        }

        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        Long memberId = member.getId();

        Long timeSlotId = request.getTimeSlotId();
        String holdToken = request.getHoldToken();
        int peopleCount = request.getPeopleCount();

        // 1. Redis HOLD 정보 검증 (단순 GET)
        String key = "hold:slot:" + timeSlotId;
        String expectedValue = memberId + ":" + holdToken;
        String actualValue = stringRedisTemplate.opsForValue().get(key);

        if (actualValue == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "선점 정보가 존재하지 않습니다.");
        }
        if (!actualValue.equals(expectedValue)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "선점 정보가 일치하지 않습니다.");
        }

        // 3. 타임슬롯 유효성 및 상태 검증
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 슬롯입니다."));

        if (timeSlot.getStatus() != TimeSlotStatus.SLOT_AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "예약 가능한 슬롯 상태가 아닙니다.");
        }

        Theme theme = timeSlot.getTheme();

        // 3.5 연령 제한 검증
        if (theme.getAgeLimit() != null && theme.getAgeLimit() > 0) {
            if (member.getAccount() == null || member.getAccount().getAge() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "나이 정보가 필요합니다.");
            }
            if (member.getAccount().getAge() < theme.getAgeLimit()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "테마 이용 연령 제한 미달입니다.");
            }
        }

        // 4. 인원 수 범위 검증
        if (peopleCount < theme.getMinPeople() || peopleCount > theme.getMaxPeople()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인원 수 범위를 초과했습니다.");
        }

        // 5. 타임슬롯 상태 변경 (SLOT_HELD로 조건부 잠금)
        int updatedRows = timeSlotRepository.updateStatus(
                timeSlotId,
                TimeSlotStatus.SLOT_HELD,
                TimeSlotStatus.SLOT_AVAILABLE,
                LocalDateTime.now()
        );

        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 사용자가 선점 중이거나 예약이 완료된 슬롯입니다.");
        }

        // 6. 예약 데이터 생성
        int totalPrice = theme.getPrice() * peopleCount;
        Reservation reservation = Reservation.builder()
                .timeSlot(timeSlot)
                .member(member)
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING_PAYMENT)
                .termsAgreedAt(LocalDateTime.now())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 7. DB 트랜잭션 커밋 성공 후 Redis HOLD key 안전 삭제 (Compare-and-Delete)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteRedisKey(key, expectedValue);
                }
            });
        } else {
            // 트랜잭션이 활성화되지 않은 환경(예: 단위 테스트)에서는 즉시 삭제 처리 수행
            deleteRedisKey(key, expectedValue);
        }

        // 8. 응답 반환
        return ReservationCreateResponse.builder()
                .reservationId(savedReservation.getId())
                .timeSlotId(timeSlotId)
                .memberId(memberId)
                .status(savedReservation.getStatus().name())
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .build();
    }

    private void deleteRedisKey(String key, String expectedValue) {
        try {
            Long result = stringRedisTemplate.execute(
                    COMPARE_AND_DELETE_SCRIPT,
                    Collections.singletonList(key),
                    expectedValue
            );
            if (result == null || result == 0) {
                log.warn("Redis 선점 정보 삭제 실패 (이미 만료되었거나 다른 선점 정보가 존재함): Key={}", key);
            } else {
                log.info("Redis 선점 정보 정상 삭제 완료: Key={}", key);
            }
        } catch (Exception e) {
            log.error("Redis 선점 정보 삭제 중 예외 발생 (예약은 정상 등록됨): Key={}", key, e);
        }
    }

    @Transactional
    public ReservationCancelResponse cancelReservation(Long reservationId) {
        // 1. 로그인 회원 정보 조회
        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));

        // 2. 예약 내역 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

        // 3. 예약 소유자 검증 (본인 예약만 취소 가능)
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 예약에 대한 권한이 없습니다.");
        }

        // 4. 취소 가능 상태 검증 (PENDING_PAYMENT, CONFIRMED 상태만 허용)
        ReservationStatus oldStatus = reservation.getStatus();
        if (oldStatus != ReservationStatus.PENDING_PAYMENT && oldStatus != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "취소 가능한 예약 상태가 아닙니다.");
        }

        // 5. 결제 및 타임슬롯 상태 복구 처리
        if (oldStatus == ReservationStatus.PENDING_PAYMENT) {
            // PENDING_PAYMENT 예약 취소 시:
            // 결제 내역이 존재하면 PAY_FAILED로 처리하고 실패 사유 기록
            Optional<Payment> paymentOpt = paymentRepository.findByReservationId(reservationId);
            paymentOpt.ifPresent(payment -> payment.fail("사용자 예약 취소로 결제 진행 중단"));

            // 타임슬롯 복구: SLOT_HELD -> SLOT_AVAILABLE
            int updatedRows = timeSlotRepository.updateStatus(
                    reservation.getTimeSlot().getId(),
                    TimeSlotStatus.SLOT_AVAILABLE,
                    TimeSlotStatus.SLOT_HELD,
                    LocalDateTime.now()
            );
            if (updatedRows == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "타임슬롯 상태 복구에 실패했습니다.");
            }
        } else {
            // CONFIRMED 예약 취소 시:
            // 결제 내역 필수 존재 및 PAY_SUCCESS 검증
            Payment payment = paymentRepository.findByReservationId(reservationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 내역을 찾을 수 없습니다."));
            if (payment.getStatus() != PaymentStatus.PAY_SUCCESS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 완료 상태의 결제 내역만 취소할 수 있습니다.");
            }

            // 결제 상태 변경: PAY_SUCCESS -> PAY_REFUND_PENDING
            payment.refundPending("사용자 예약 취소로 인한 환불 대기");

            // 타임슬롯 복구: SLOT_FULL -> SLOT_AVAILABLE
            int updatedRows = timeSlotRepository.updateStatus(
                    reservation.getTimeSlot().getId(),
                    TimeSlotStatus.SLOT_AVAILABLE,
                    TimeSlotStatus.SLOT_FULL,
                    LocalDateTime.now()
            );
            if (updatedRows == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "타임슬롯 상태 복구에 실패했습니다.");
            }
        }

        // 6. 예약 상태 변경 및 저장
        reservation.cancel();
        Reservation savedReservation = reservationRepository.save(reservation);

        Long paymentId = null;
        if (oldStatus == ReservationStatus.CONFIRMED) {
            Payment payment = paymentRepository.findByReservationId(reservationId).orElse(null);
            if (payment != null) {
                paymentId = payment.getId();
            }
        }

        return ReservationCancelResponse.builder()
                .reservationId(savedReservation.getId())
                .status(savedReservation.getStatus().name())
                .paymentId(paymentId)
                .build();
    }
}
