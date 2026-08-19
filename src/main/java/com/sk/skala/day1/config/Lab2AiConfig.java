package com.sk.skala.day1.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Lab2AiConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // lab1의 summaryChatClient는 maxTokens=120이라 구조화 응답(JSON)이 잘린다.
    @Bean
    public ChatClient lab2ChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.0)
                        .maxTokens(500))
                .build();
    }
}
