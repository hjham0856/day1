package com.sk.skala.day1.advisor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

import reactor.core.publisher.Flux;

public class SafetyAdvisor implements CallAdvisor, StreamAdvisor {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final int MAX_INPUT_LENGTH = 4_000;
    private static final Pattern RESIDENT_NUMBER =
            Pattern.compile("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String rejection = rejectionMessage(request.prompt().getUserMessage().getText());
        return rejection == null ? chain.nextCall(request) : rejected(request, rejection);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String rejection = rejectionMessage(request.prompt().getUserMessage().getText());
        return rejection == null
                ? chain.nextStream(request)
                : Flux.just(rejected(request, rejection));
    }

    private String rejectionMessage(String input) {
        String text = input == null ? "" : input;
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (text.length() > MAX_INPUT_LENGTH) {
            return "질문은 4,000자 이하로 입력해 주세요.";
        }
        if (RESIDENT_NUMBER.matcher(text).find()) {
            return "주민등록번호 같은 개인정보는 입력할 수 없습니다.";
        }
        if (normalized.contains("이전 지시") || normalized.contains("시스템 프롬프트")
                || normalized.contains("ignore previous")) {
            return "내부 지시나 시스템 프롬프트는 제공할 수 없습니다.";
        }
        if (normalized.contains("다른 고객") && (normalized.contains("이름") || normalized.contains("주소"))) {
            return "다른 고객의 정보는 제공할 수 없습니다.";
        }
        if ((normalized.contains("전부") || normalized.contains("모두"))
                && normalized.contains("환불")) {
            return "환불은 주문번호별로 한 건씩 접수할 수 있습니다.";
        }
        return null;
    }

    private ChatClientResponse rejected(ChatClientRequest request, String message) {
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(message))))
                        .build())
                .context(Map.copyOf(request.context()))
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
