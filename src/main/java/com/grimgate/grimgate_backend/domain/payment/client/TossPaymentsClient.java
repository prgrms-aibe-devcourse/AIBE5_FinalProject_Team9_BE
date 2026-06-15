package com.grimgate.grimgate_backend.domain.payment.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TossPaymentsClient {

    private final WebClient webClient;

    @Value("${toss.secret-key}")
    private String secretKey;

    public TossPaymentsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.tosspayments.com")
                .build();
    }

    /**
     * 토스페이먼츠 결제 승인 API를 연동하여 호출합니다.
     * 
     * [이슈 #56 비즈니스 요구사항]
     * - 인증에 필요한 Authorization 헤더를 Base64 인코딩을 거친 시크릿 키값으로 전송합니다.
     * - 외부 API 호출로 발생하는 예외는 상위 서비스 계층으로 전파하여 트랜잭션 분리 처리에 활용합니다.
     *
     * @param paymentKey PG사 결제 식별 키
     * @param orderId 우리 서비스 고유 식별 주문 ID
     * @param amount 결제 요청 금액
     * @return 토스페이먼츠 API 승인 완료 응답 객체
     */
    public TossConfirmResponseDto confirm(String paymentKey, String orderId, Integer amount) {
        String basicAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return webClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .bodyToMono(TossConfirmResponseDto.class)
                .block();
    }

    private record TossConfirmRequest(String paymentKey, String orderId, Integer amount) {}

    public record TossConfirmResponseDto(
            String paymentKey,
            String orderId,
            String method,
            String approvedAt
    ) {}

    /**
     * 토스페이먼츠 결제 취소(환불) API를 연동하여 호출합니다.
     *
     * @param paymentKey PG사 결제 식별 키
     * @param cancelReason 환불 사유
     * @return 토스페이먼츠 API 취소 완료 응답 객체
     */
    public TossCancelResponseDto cancel(String paymentKey, String cancelReason) {
        String basicAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return webClient.post()
                .uri("/v1/payments/" + paymentKey + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TossCancelRequest(cancelReason))
                .retrieve()
                .bodyToMono(TossCancelResponseDto.class)
                .block();
    }

    private record TossCancelRequest(String cancelReason) {}

    public record TossCancelResponseDto(
            String paymentKey,
            String orderId,
            String status,
            java.util.List<TossCancelDetail> cancels
    ) {
        public record TossCancelDetail(
                Integer cancelAmount,
                String cancelReason,
                String canceledAt
        ) {}
    }
}
