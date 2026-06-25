package com.grimgate.grimgate_backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 서비스 전반에서 사용하는 에러 코드 목록
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증/계정
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "서비스 이용약관에 동의해야 합니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REVOKED_TOKEN(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),

    // 회원/역할
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버 정보를 찾을 수 없습니다."),
    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "매니저 정보를 찾을 수 없습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 역할입니다."),

    // 프로필/마이페이지
    PROFILE_CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필 캐릭터를 찾을 수 없습니다."),
    TITLE_NOT_FOUND(HttpStatus.NOT_FOUND, "칭호를 찾을 수 없습니다."),

    //마이페이지 후기
    INVALID_RESERVATION_TYPE(HttpStatus.BAD_REQUEST, "예약 조회 타입은 UPCOMING 또는 PAST만 허용됩니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    RESERVATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 예약만 후기 작성이 가능합니다."),
    RESERVATION_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "확정된 예약만 결과를 입력할 수 있습니다."),
    RESERVATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 결과가 입력된 예약입니다."),
    CLEAR_TIME_REQUIRED(HttpStatus.BAD_REQUEST, "클리어 성공 시 클리어 시간은 필수입니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 후기를 작성한 예약입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."),
    REVIEW_NOT_OWNER(HttpStatus.FORBIDDEN, "본인의 후기만 수정/삭제할 수 있습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 3장까지 등록 가능합니다."),
    REVIEW_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 후기입니다."),
    REVIEW_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."),
    INVALID_REVIEW_REPORT_STATUS(HttpStatus.BAD_REQUEST, "현재 상태에서는 해당 처리를 수행할 수 없습니다."),
    REPORTED_REVIEW_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "신고 접수된 후기는 삭제할 수 없습니다."),

    // 지점/테마 (사장님 페이지)
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."),
    BRANCH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 지점에 대한 권한이 없습니다."),
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "테마를 찾을 수 없습니다."),

    // 메이트 모집
    MATE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "메이트 모집글을 찾을 수 없습니다."),
    MATE_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 사용자의 메이트 모집글입니다."),
    MATE_POST_INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "마감일은 모임 시간보다 늦을 수 없습니다."),
    MATE_POST_INVALID_MEETING_TIME(HttpStatus.BAD_REQUEST, "모임 시간은 현재 이후여야 합니다."),
    MATE_POST_INVALID_OPEN_CHAT_URL(HttpStatus.BAD_REQUEST, "카카오 오픈채팅 URL 형식이 올바르지 않습니다."),
    MATE_POST_CANNOT_CLOSE(HttpStatus.BAD_REQUEST, "모집 중인 상태에서만 마감할 수 있습니다."),

    // 메이트 댓글
    MATE_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    MATE_COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글 작성자만 수정/삭제할 수 있습니다."),
    MATE_COMMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    MATE_REPLY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대댓글에는 대댓글을 작성할 수 없습니다."),
    MATE_REPLY_POST_MISMATCH(HttpStatus.BAD_REQUEST, "다른 게시글의 댓글에는 대댓글을 작성할 수 없습니다."),
    MATE_REPLY_TO_DELETED_COMMENT(HttpStatus.BAD_REQUEST, "삭제된 댓글에는 대댓글을 작성할 수 없습니다."),

    // 메이트 참여
    MATE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 정보를 찾을 수 없습니다."),
    MATE_PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 모집글입니다."),
    MATE_PARTICIPANT_AUTHOR_CANNOT_JOIN(HttpStatus.BAD_REQUEST, "작성자는 자신의 모집글에 참여할 수 없습니다."),
    MATE_PARTICIPANT_FULL(HttpStatus.CONFLICT, "모집 인원이 모두 찼습니다."),
    MATE_PARTICIPANT_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "현재 참여 가능한 상태가 아닙니다."),
    MATE_PARTICIPANT_NOT_JOINED(HttpStatus.BAD_REQUEST, "참여 중이 아닙니다."),
    MATE_PARTICIPANT_KICK_FORBIDDEN(HttpStatus.FORBIDDEN, "작성자만 참여자를 내보낼 수 있습니다."),
    MATE_PARTICIPANT_KICK_SELF(HttpStatus.BAD_REQUEST, "작성자 본인은 강퇴할 수 없습니다."),
    MATE_PARTICIPANT_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "모집 마감이 지난 모집글에는 참여할 수 없습니다."),
    MATE_PARTICIPANT_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "모집이 완료되었거나 마감된 모집글은 참여를 취소할 수 없습니다."),
    MATE_PARTICIPANT_LIST_FORBIDDEN(HttpStatus.FORBIDDEN, "참여자 목록은 작성자만 조회할 수 있습니다."),

    INVALID_THEME_CAPACITY(HttpStatus.BAD_REQUEST, "최소 인원은 최대 인원보다 클 수 없습니다."),

    // 결제
    INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "결제 가능한 예약 상태가 아닙니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 요청 금액이 예약 금액과 일치하지 않습니다."),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 예약에 대한 결제 내역이 이미 존재합니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "결제 승인이 가능한 상태가 아닙니다."),
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "웹훅 서명 검증에 실패했습니다."),
    INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "환불이 가능한 결제 상태가 아닙니다."),
    PAYMENT_KEY_MISSING(HttpStatus.BAD_REQUEST, "결제 고유 키(paymentKey)가 존재하지 않습니다."),

    // 엑셀 내보내기
    EXCEL_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}