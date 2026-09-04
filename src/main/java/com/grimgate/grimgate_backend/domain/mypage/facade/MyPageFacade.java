package com.grimgate.grimgate_backend.domain.mypage.facade;

import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageMainResponse;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageProfileResponse;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageStatsResponse;
import com.grimgate.grimgate_backend.domain.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 마이페이지 메인 화면 조합을 담당하는 파사드
 */
@Component
@RequiredArgsConstructor
public class MyPageFacade {

    private final MyPageService myPageService;

    /**
     * 마이페이지 메인 조회: 프로필 + 통계 통합 반환
     */
    public MyPageMainResponse getMyPageMain(Long accountId) {
        MyPageStatsResponse stats = myPageService.getStats(accountId);
        MyPageProfileResponse profile = myPageService.getProfile(accountId);

        return MyPageMainResponse.builder()
                .profile(profile)
                .stats(stats)
                .build();
    }
}
