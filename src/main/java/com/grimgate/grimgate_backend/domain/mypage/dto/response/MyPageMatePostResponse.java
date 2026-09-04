package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePostStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyPageMatePostResponse {

    private Long matePostId;
    private String title;
    private MatePostStatus status;
    private LocalDateTime meetingTime;
    private int currentPeople;
    private int maxPeople;
    private LocalDateTime createdAt;
    // 분위기 태그 (콤마로 직렬화된 문자열)
    private String tags;
    private String imageUrl;

    public static MyPageMatePostResponse from(MatePost matePost) {
        return MyPageMatePostResponse.builder()
                .matePostId(matePost.getId())
                .title(matePost.getTitle())
                .status(matePost.getStatus())
                .meetingTime(matePost.getMeetingTime())
                .currentPeople(matePost.getCurrentPeople())
                .maxPeople(matePost.getMaxPeople())
                .createdAt(matePost.getCreatedAt())
                .tags(matePost.getTags())
                .imageUrl(matePost.getImageUrl())
                .build();
    }
}
