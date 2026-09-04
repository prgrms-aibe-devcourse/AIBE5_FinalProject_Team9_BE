package com.grimgate.grimgate_backend.domain.payment.entity;

/**
 * 결제 상태 및 결제 생명주기(Lifecycle) 흐름을 정의하는 Enum 클래스입니다.
 * 
 * [결제 흐름 설명]
 * 1. PAY_PENDING (결제 대기): ready API를 호출하여 결제 요청 데이터를 생성하고 초기화한 상태입니다. 사용자가 토스 페이먼츠 결제창을 통해 결제 승인을 요청하기 직전 단계입니다.
 * 2. PAY_SUCCESS (결제 성공): 결제 승인 API(confirm) 호출이 정상적으로 완료되어 PG사로부터 승인이 떨어진 상태입니다. (confirm API 구현은 후속 이슈 범위)
 * 3. PAY_FAILED (결제 실패): 결제 인증 또는 승인 과정에서 PG사 에러나 한도 초과 등의 사유로 결제가 취소된 상태입니다.
 * 4. PAY_REFUNDED (결제 환불): 결제가 성공한 상태에서 사용자가 예약을 취소하거나 관리자에 의해 전액/부분 환불이 완료된 상태입니다. (환불 구현은 후속 이슈 범위)
 * 5. PAYMENT_TIMEOUT (결제 시간 초과): 결제 대기 상태(PAY_PENDING)에서 일정 시간(예: 임시 선점 5분) 동안 승인이 완료되지 않아 자동으로 만료 처리된 상태입니다.
 */
public enum PaymentStatus {
    /**
     * 결제 요청이 생성되어 PG사 승인을 기다리는 상태 (ready API 완료 시 설정)
     */
    PAY_PENDING,

    /**
     * PG사로부터 최종 결제 승인이 완료된 상태 (confirm 완료 시 설정)
     */
    PAY_SUCCESS,

    /**
     * 한도 초과, 잔액 부족 등 결제 승인이 실패한 상태
     */
    PAY_FAILED,

    /**
     * 예약 취소 등에 의해 환불(취소) 처리가 완료된 상태
     */
    PAY_REFUNDED,

    // 결제 성공 후 예약 취소된 건을 실제 환불 전까지 구분하기 위한 대기 상태
    PAY_REFUND_PENDING,

    /**
     * 임시 선점 시간 만료 등으로 결제가 진행되지 않고 초과된 상태
     */
    PAYMENT_TIMEOUT
}
