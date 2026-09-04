package com.grimgate.grimgate_backend.domain.auth.dto;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String gender;
    private Integer age;
    private boolean ageVisible;
    private boolean genderVisible;
    private boolean emailVisible;
    private String storeName;

    public static MeResponse from(Account account) {
        return from(account, null);
    }

    // Account 엔티티로부터 MeResponse 생성
    public static MeResponse from(Account account, String storeName) {
        return MeResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .role(account.getRole().name())
                .gender(account.getGender())
                .age(account.getAge())
                .ageVisible(account.isAgeVisible())
                .genderVisible(account.isGenderVisible())
                .emailVisible(account.isEmailVisible())
                .storeName(storeName)
                .build();
    }
}
