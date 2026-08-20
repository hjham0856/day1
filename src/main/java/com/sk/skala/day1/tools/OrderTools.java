package com.sk.skala.day1.tools;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.sk.skala.day1.advisor.AuditAdvisor;
import com.sk.skala.day1.repository.OrderRepository;
import com.sk.skala.day1.web.OrderView;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderRepository orders;
    private final MeterRegistry registry;

    @Tool(description = """
            주문 상태를 조회한다. 사용자가 주문번호를 말하거나
            '내 주문', '배송 언제'처럼 물으면 이 도구를 쓴다.
            주문번호가 없으면 먼저 주문번호를 물어본다.
            """)
    public OrderView getOrder(
            @ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
            ToolContext context) {
        String userId = (String) context.getContext().get("userId");
        String traceId = String.valueOf(context.getContext().getOrDefault(AuditAdvisor.TRACE_ID, "unknown"));

        try {
            checkCallLimit(context);
            OrderView result = orders.findByIdAndOwnerId(orderId, userId)
                    .map(OrderView::from)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
            registry.counter("ai.tool.calls", "tool", "getOrder", "result", "ok").increment();
            log.info("[{}] tool=getOrder orderId={} userId={} result=ok", traceId, orderId, userId);
            return result;
        }
        catch (RuntimeException e) {
            registry.counter("ai.tool.calls", "tool", "getOrder", "result", "fail").increment();
            log.warn("[{}] tool=getOrder orderId={} userId={} result=fail", traceId, orderId, userId);
            throw e;
        }
    }

    private static void checkCallLimit(ToolContext context) {
        Object value = context.getContext().get("toolCalls");
        if (value instanceof AtomicInteger calls && calls.incrementAndGet() > 5) {
            throw new IllegalStateException("도구 호출 상한을 초과했습니다.");
        }
    }
}
