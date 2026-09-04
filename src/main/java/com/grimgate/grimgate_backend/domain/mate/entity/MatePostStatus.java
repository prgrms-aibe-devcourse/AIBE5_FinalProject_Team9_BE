package com.grimgate.grimgate_backend.domain.mate.entity;

/**
 * 메이트 모집글 상태값.
 *
 * 명세서(GrimGate 기능명세서 - 2_상태값 정의) 기준:
 * - DRAFT         : 임시저장
 * - RECRUITING    : 모집 중
 * - CLOSING_SOON  : 마감 임박 (마감 24시간 이내 또는 1자리 남음)
 * - CLOSED        : 모집 마감 (마감일 경과 / 작성자 수동 마감)
 * - MATCHED       : 매칭 완료 (모집 인원 충족 시 자동 전이)
 * - DELETED       : 삭제됨 (soft delete)
 */
public enum MatePostStatus {
    DRAFT,
    RECRUITING,
    CLOSING_SOON,
    CLOSED,
    MATCHED,
    DELETED
}
