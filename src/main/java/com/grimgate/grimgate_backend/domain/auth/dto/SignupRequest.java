package com.grimgate.grimgate_backend.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    private String nickname;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "비밀번호는 8자 이상, 숫자와 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다. (예: 01012345678)")
    private String phone;

    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별 값이 올바르지 않습니다.")
    private String gender;

    @Min(value = 0, message = "나이는 0 이상이어야 합니다.")
    @Max(value = 150, message = "나이는 150 이하여야 합니다.")
    private Integer age;

    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private boolean termsAgreed;

    private boolean marketingAgreed = false;

    // password와 passwordConfirm 일치 검증
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmValid() {
        if (password == null || passwordConfirm == null) return false;
        return password.equals(passwordConfirm);
    }
}
