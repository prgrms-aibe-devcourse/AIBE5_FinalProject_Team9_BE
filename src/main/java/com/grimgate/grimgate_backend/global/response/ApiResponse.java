package com.grimgate.grimgate_backend.global.response;

import lombok.Getter;

// 모든 API 응답을 통일된 형식으로 감싸는 클래스
@Getter
public class ApiResponse<T> {

    // 요청 성공 여부
    private final boolean success;

    // 응답 메시지
    private final String message;

    // 응답 데이터 (실패 시 null)
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 성공 응답 (데이터만)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공했습니다.", data);
    }

    // 성공 응답 (메시지 + 데이터)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // 실패 응답 (메시지만)
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
