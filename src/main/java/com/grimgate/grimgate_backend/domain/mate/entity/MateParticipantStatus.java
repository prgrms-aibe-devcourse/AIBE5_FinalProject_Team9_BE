package com.grimgate.grimgate_backend.domain.mate.entity;

/**
 * 메이트 참여자 상태값.
 *
 * <ul>
 *   <li>JOINED   : 참여 중 (활성)</li>
 *   <li>CANCELLED: 참여 취소 (본인이 취소)</li>
 *   <li>KICKED   : 강퇴 (작성자가 내보냄)</li>
 * </ul>
 */
public enum MateParticipantStatus {
    JOINED,
    CANCELLED,
    KICKED
}
