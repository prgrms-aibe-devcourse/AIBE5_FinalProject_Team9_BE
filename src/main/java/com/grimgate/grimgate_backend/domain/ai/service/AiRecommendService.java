package com.grimgate.grimgate_backend.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.ai.client.GeminiClient;
import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendRequest;
import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private final GeminiClient geminiClient;
    private final ThemeRepository themeRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI-001: 대화 기반 테마 추천
     */
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        String userMessage = extractLastUserMessage(request);

        // Java에서 먼저 필터링 후 최대 1개만 Gemini에 전달
        List<Theme> filteredThemes = filterThemesByKeyword(userMessage);

        String systemPrompt = buildSystemPrompt(filteredThemes);

        List<Map<String, Object>> contents = request.messages().stream()
                .map(msg -> Map.<String, Object>of(
                        "role", msg.role() == AiRecommendRequest.Role.assistant ? "model" : "user",
                        "parts", List.of(Map.of("text", msg.content()))
                ))
                .collect(Collectors.toList());

        try {
            String geminiResponse = geminiClient.call(contents, systemPrompt);
            return parseGeminiResponse(geminiResponse);
        } catch (Exception e) {
            log.warn("Gemini 호출 실패, fallback 실행: {}", e.getMessage());
            return AiRecommendResponse.fallback(filteredThemes);
        }
    }

    /**
     * AI-002: 랜덤 테마 1개 추천 (Gemini 호출 없음)
     */
    public List<AiRecommendResponse.ThemeCard> random() {
        return themeRepository.findRandom(1)
                .stream()
                .map(AiRecommendResponse.ThemeCard::from)
                .toList();
    }

    // ── private 메서드 ──────────────────────────────────────────

    /**
     * 사용자 메시지 키워드로 DB에서 먼저 필터링 후 최대 3개 반환
     */
    private List<Theme> filterThemesByKeyword(String userMessage) {
        List<Theme> themes;

        if (userMessage.contains("극한") || (userMessage.contains("무서운") && !userMessage.contains("못"))) {
            themes = themeRepository.findByHorrorLevel(5);
        } else if (userMessage.contains("약한") || userMessage.contains("무섭지 않은") || userMessage.contains("가벼운")
                || (userMessage.contains("무서운") && userMessage.contains("못"))) {
            themes = themeRepository.findByHorrorLevelLessThanEqual(2);
        } else if (userMessage.contains("어려운") || userMessage.contains("난이도 높은") || userMessage.contains("고난이도")) {
            themes = themeRepository.findByDifficultyGreaterThanEqual(4);
            Collections.shuffle(themes);
        } else if (userMessage.contains("쉬운") || userMessage.contains("초보") || userMessage.contains("입문")) {
            themes = themeRepository.findByDifficultyLessThanEqual(2);
        } else if (userMessage.contains("스릴러")) {
            themes = themeRepository.findByTagsContaining("스릴러");
            Collections.shuffle(themes);
        } else if (userMessage.contains("추리")) {
            themes = themeRepository.findByTagsContaining("추리");
            Collections.shuffle(themes);
        } else if (userMessage.contains("미스터리")) {
            themes = themeRepository.findByTagsContaining("미스터리");
            Collections.shuffle(themes);
        }  else if (userMessage.contains("둘이") || userMessage.contains("혼자")) {
            int people = userMessage.contains("혼자") ? 1 : 2;
            themes = themeRepository.findByMinPeopleLessThanEqual(people);
        } else if (userMessage.contains("1시간") || userMessage.contains("60분")) {
            themes = themeRepository.findByPlayTimeBetween(55, 65);
        } else {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)명").matcher(userMessage);
            if (matcher.find()) {
                int people = Integer.parseInt(matcher.group(1));
                themes = themeRepository.findByMinPeopleLessThanEqual(people);
            } else {
                //조건 없으면 랜덤 1개
                themes = themeRepository.findRandom(1);
            }
        }

        // 최대 개만 Gemini에 전달
        return themes.stream().limit(1).toList();
    }

    private String extractLastUserMessage(AiRecommendRequest request) {
        return request.messages().stream()
                .filter(m -> m.role() == AiRecommendRequest.Role.user)
                .reduce((first, second) -> second)
                .map(AiRecommendRequest.Message::content)
                .orElse("");
    }

    private String buildSystemPrompt(List<Theme> filteredThemes) {
        String systemInstruction = """
                너는 방탈출 테마 추천 전문가야.
                사용자의 요구사항에 맞춰 1차로 엄선된 테마 목록이야. 반드시 1개의 테마를 추천해줘. 반드시 정확히 1개만 추천해.
                추천 메시지 작성 시 다음 규칙을 반드시 지켜:
                - '유일한', '목록에 하나뿐', '이 테마만 존재' 같은 표현 절대 사용 금지
                - 그냥 자연스럽게 테마를 추천하는 이유만 간결하게 설명해
                - 예시: "'실험 섬'을 추천해 드립니다. 극한의 공포와 생존 스릴을 동시에 경험할 수 있는 테마입니다."
                이 중에서 사용자의 의도에 가장 잘 맞는 테마를 선택해서 추천 사유와 함께 JSON으로 반환해줘.
                응답 형식을 절대 벗어나지 마. 다른 텍스트는 절대 포함하지 마.
                
                추천할 경우: {"type": "recommendation", "theme_ids": [1], "message": "사용자 맞춤 추천 이유"}
                추천 불가 시: {"type": "message", "message": "부드러운 대화 답변"}
                
                엄선된 후보 테마 목록:
                """;

        StringBuilder sb = new StringBuilder(systemInstruction);

        for (Theme theme : filteredThemes) {
            String shortDesc = theme.getDescription().length() > 50
                    ? theme.getDescription().substring(0, 50) + "..."
                    : theme.getDescription();

            sb.append(String.format("ID: %d | 제목: %s | 태그: %s | 난이도: %d | 공포도: %d | 플레이시간: %d분 | 최소인원: %d | 최대인원: %d |설명: %s\n",
                    theme.getId(),
                    theme.getTitle(),
                    theme.getTags(),
                    theme.getDifficulty(),
                    theme.getHorrorLevel(),
                    theme.getPlayTime(),
                    theme.getMinPeople(),
                    theme.getMaxPeople(),
                    shortDesc
            ));
        }

        return sb.toString();
    }

    private AiRecommendResponse parseGeminiResponse(String geminiText) {
        try {
            String cleanJson = geminiText.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(cleanJson);

            String type = root.path("type").asText();
            String message = root.path("message").asText();

            if ("recommendation".equals(type)) {
                List<Long> themeIds = objectMapper.convertValue(
                        root.path("theme_ids"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
                );
                List<Theme> recommended = themeRepository.findAllById(themeIds);
                return AiRecommendResponse.recommend(message, recommended);
            } else {
                return AiRecommendResponse.message(message);
            }

        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패: {}", e.getMessage());
            return AiRecommendResponse.fallback(themeRepository.findRandom(1));
        }
    }
}