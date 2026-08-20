package com.sk.skala.day1.tools;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.sk.skala.day1.advisor.AuditAdvisor;
import com.sk.skala.day1.chat.RefundTicketService;
import com.sk.skala.day1.chat.TicketView;
import com.sk.skala.day1.repository.OrderRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketTools {

    private final OrderRepository orders;
    private final RefundTicketService tickets;
    private final MeterRegistry registry;

    @Tool(description = "환불을 접수한다. 즉시 처리되지 않고 담당자 승인 후 처리된다.")
    public TicketView requestRefund(
            @ToolParam(description = "주문번호. 예: 12345") String orderId,
            @ToolParam(description = "환불 사유. 예: 단순 변심") String reason,
            ToolContext context) {
        String userId = (String) context.getContext().get("userId");
        String traceId = String.valueOf(context.getContext().getOrDefault(AuditAdvisor.TRACE_ID, "unknown"));

        try {
            checkCallLimit(context);
            orders.findByIdAndOwnerId(orderId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
            TicketView ticket = tickets.create(orderId, userId, reason);
            registry.counter("ai.tool.calls", "tool", "requestRefund", "result", "ok").increment();
            log.info("[{}] tool=requestRefund orderId={} userId={} ticket={} result=ok",
                    traceId, orderId, userId, ticket.no());
            return ticket;
        }
        catch (RuntimeException e) {
            registry.counter("ai.tool.calls", "tool", "requestRefund", "result", "fail").increment();
            log.warn("[{}] tool=requestRefund orderId={} userId={} result=fail", traceId, orderId, userId);
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
