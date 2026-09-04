package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageProfileResponse {

    private String nickname;

    // 칭호명 (없으면 null)
    private String titleName;

    // 비공개면 null
    private String gender;

    // 비공개면 null
    private Integer age;

    private String profileCharacterImageUrl;
}
