package com.grimgate.grimgate_backend.domain.mypage.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MyPageProfileUpdateRequest {

    private String nickname;

    private Integer age;

    private String gender;

    private Boolean ageVisible;

    private Boolean genderVisible;

    private Boolean emailVisible;
}
