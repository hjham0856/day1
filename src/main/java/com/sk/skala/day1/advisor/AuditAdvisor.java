package com.sk.skala.day1.advisor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;

public class AuditAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);
    private static final Pattern RESIDENT_NUMBER = Pattern.compile("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)");

    private final MeterRegistry registry;

    public AuditAdvisor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long started = System.nanoTime();
        logRequest(request);
        try {
            ChatClientResponse response = chain.nextCall(request);
            record(response, started, "ok");
            return response;
        }
        catch (RuntimeException e) {
            record(null, started, "fail");
            log.warn("[{}] chat result=fail type={}", traceId(request), e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return Flux.defer(() -> {
            long started = System.nanoTime();
            AtomicReference<ChatClientResponse> last = new AtomicReference<>();
            logRequest(request);
            return chain.nextStream(request)
                    .doOnNext(last::set)
                    .doOnComplete(() -> record(last.get(), started, "ok"))
                    .doOnError(error -> record(null, started, "fail"));
        });
    }

    private void logRequest(ChatClientRequest request) {
        String question = request.prompt().getUserMessage().getText();
        String safeQuestion = RESIDENT_NUMBER.matcher(question == null ? "" : question)
                .replaceAll("******-*******");
        if (safeQuestion.length() > 200) {
            safeQuestion = safeQuestion.substring(0, 200) + "...";
        }
        log.info("[{}] user={} question=\"{}\"", traceId(request),
                request.context().getOrDefault(USER_ID, "unknown"), safeQuestion);
    }

    private void record(ChatClientResponse response, long started, String result) {
        long elapsed = System.nanoTime() - started;
        registry.timer("ai.latency", "phase", "chat", "result", result)
                .record(elapsed, TimeUnit.NANOSECONDS);

        int promptTokens = 0;
        int completionTokens = 0;
        if (response != null && response.chatResponse() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();
            promptTokens = value(usage.getPromptTokens());
            completionTokens = value(usage.getCompletionTokens());
        }
        registry.counter("ai.tokens", "type", "prompt", "feature", "chat")
                .increment(promptTokens);
        registry.counter("ai.tokens", "type", "completion", "feature", "chat")
                .increment(completionTokens);

        String traceId = response == null
                ? "unknown"
                : String.valueOf(response.context().getOrDefault(TRACE_ID, "unknown"));
        log.info("[{}] chat result={} latencyMs={} promptTokens={} completionTokens={}",
                traceId, result, TimeUnit.NANOSECONDS.toMillis(elapsed), promptTokens, completionTokens);
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static String traceId(ChatClientRequest request) {
        return String.valueOf(request.context().getOrDefault(TRACE_ID, "unknown"));
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
