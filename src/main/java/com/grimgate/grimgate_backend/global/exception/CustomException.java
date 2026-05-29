package com.grimgate.grimgate_backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 서비스 전반에서 공통으로 사용하는 커스텀 예외 클래스
@Getter
public class CustomException extends RuntimeException {

    // HTTP 상태 코드
    private final HttpStatus httpStatus;

    // 사용자에게 전달할 에러 메시지
    private final String message;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus();
        this.message = errorCode.getMessage();
    }
    public CustomException(HttpStatus httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.message = message;
        }
    }
