package com.sk.skala.day1.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Lab1AiConfig {

    @Bean
    public ChatClient summaryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        당신은 이커머스 주문 상태 도우미다.
                        주어진 주문 정보만 사용해 답변한다.
                        주문 정보에 없는 내용은 추측하지 않는다.
                        항상 한국어 한 문장으로만 요약한다.
                        """)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.0)
                        .maxTokens(120))
                .build();
    }
}
