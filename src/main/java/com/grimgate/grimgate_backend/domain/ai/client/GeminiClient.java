package com.grimgate.grimgate_backend.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    public GeminiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Gemini API를 호출해 응답 텍스트를 반환합니다.
     *
     * @param contents Gemini contents 배열 (role: user/model, parts: [{text: ...}])
     * @param systemPrompt 시스템 프롬프트 (테마 목록 컨텍스트 포함)
     * @return Gemini 응답 텍스트 (JSON 문자열)
     */
    public String call(List<Map<String, Object>> contents, String systemPrompt) {
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", contents
        );


       try{
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                   // 429(TooManyRequests)가 '아닐 때만' 재시도하도록 필터 변경
                   .filter(throwable -> !(throwable instanceof WebClientResponseException.TooManyRequests))
           )
                .block();

        return extractText(response);
       } catch (WebClientResponseException.TooManyRequests e) {
           System.err.println("Gemini API 호출 한도 초과 (429): " + e.getMessage());
           throw e;  // 텍스트 반환 말고 예외 던지기
       }  catch (Exception e) {
           System.err.println("Gemini API 호출 중 알 수 없는 에러 발생: " + e.getMessage());
           throw e;  // 이것도 예외 던지기
       }
    }

    private String extractText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Gemini 응답 파싱 실패", e);
        }
    }
}
