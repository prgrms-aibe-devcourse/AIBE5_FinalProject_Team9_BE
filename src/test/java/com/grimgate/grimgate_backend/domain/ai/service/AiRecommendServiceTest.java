package com.grimgate.grimgate_backend.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.ai.client.GeminiClient;
import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendRequest;
import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiRecommendServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiRecommendService aiRecommendService;


    @Test
    @DisplayName("Ai 성공 시 정상 음답")
    void 무서운_키워드_입력시_horrorLevel5_테마_조회() {
        Branch branch = Branch.builder()
                .branchName("테스트 지점")
                .region("서울")
                .build();

        Theme theme = Theme.builder()
                .id(1L)
                .tags("공포,스릴러")
                .horrorLevel(5)
                .difficulty(5)
                .description("폐병원에 갇힌 채 새벽 6시까지 살아남아라")
                .branch(branch)   // 추가
                .build();

        when(themeRepository.findByHorrorLevel(5)).thenReturn(List.of(theme));
        AiRecommendRequest request = new AiRecommendRequest(
                List.of(new AiRecommendRequest.Message(AiRecommendRequest.Role.user, "무서운 거 추천해줘"))
        );

        // when
        aiRecommendService.recommend(request);

        // then
        verify(themeRepository).findByHorrorLevel(5); // 이 메서드가 호출했는지


    }

    @Test
    void Gemini_실패시_fallback_동작() {
        // given
        Branch branch = Branch.builder()
                .branchName("테스트 지점")
                .region("서울")
                .build();

        Theme theme = Theme.builder()
                .id(1L)
                .tags("공포,스릴러")
                .horrorLevel(5)
                .difficulty(5)
                .description("폐병원에 갇힌 채 새벽 6시까지 살아남아라")
                .branch(branch)   // 추가
                .build();

        when(themeRepository.findByHorrorLevel(5)).thenReturn(List.of(theme));
        when(geminiClient.call(any(), any())).thenThrow(new RuntimeException("Gemini 호출 실패"));

        AiRecommendRequest request = new AiRecommendRequest(
                List.of(new AiRecommendRequest.Message(AiRecommendRequest.Role.user, "무서운 거 추천해줘"))
        );

        // when
        AiRecommendResponse response = aiRecommendService.recommend(request);

        // then
        assertThat(response.type()).isEqualTo("recommendation"); //fallback 응답이 올바른지
    }


}
