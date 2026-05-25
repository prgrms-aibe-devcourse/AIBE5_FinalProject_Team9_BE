package com.grimgate.grimgate_backend.global.response;

//API 응답 형식 통일

/* 사용예시
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}

컨트롤러 예시
@GetMapping("/{id}")
public ApiResponse<ThemeResponse> getTheme(@PathVariable Long id) {

    ThemeResponse response = themeService.getTheme(id);

    return new ApiResponse<>(
            true,
            "조회 성공",
            response
    );
}
*/