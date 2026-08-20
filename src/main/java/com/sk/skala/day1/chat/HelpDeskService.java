package com.sk.skala.day1.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sk.skala.day1.advisor.AuditAdvisor;

@Service
public class HelpDeskService {

    private final ChatClient chatClient;
    private final ChatMemory memory;

    public HelpDeskService(
            @Qualifier("assistantChatClient") ChatClient chatClient,
            @Qualifier("lab3ChatMemory") ChatMemory memory) {
        this.chatClient = chatClient;
        this.memory = memory;
    }

    public AnswerDto chat(String userId, String sessionId, String message) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        ChatClientResponse response = chatClient.prompt()
                .user(message)
                .advisors(advisors -> advisors
                        .param(ChatMemory.CONVERSATION_ID, conversationId(userId, sessionId))
                        .param(AuditAdvisor.TRACE_ID, traceId)
                        .param(AuditAdvisor.USER_ID, userId))
                .toolContext(Map.of(
                        "userId", userId,
                        AuditAdvisor.TRACE_ID, traceId,
                        "toolCalls", new AtomicInteger()))
                .call()
                .chatClientResponse();

        String answer = response.chatResponse().getResult().getOutput().getText();
        return new AnswerDto(answer, sources(response));
    }

    public List<HistoryMessage> history(String userId, String sessionId) {
        return memory.get(conversationId(userId, sessionId)).stream()
                .map(message -> new HistoryMessage(
                        message.getMessageType().name().toLowerCase(),
                        message.getText()))
                .toList();
    }

    private static String conversationId(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    @SuppressWarnings("unchecked")
    private static List<String> sources(ChatClientResponse response) {
        Object value = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (!(value instanceof List<?>)) {
            return List.of();
        }
        return ((List<Document>) value).stream()
                .map(document -> String.valueOf(document.getMetadata().get("source")))
                .distinct()
                .toList();
    }
}
