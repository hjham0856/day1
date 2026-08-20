package com.sk.skala.day1.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.sk.skala.day1.repository.OrderRepository;
import com.sk.skala.day1.web.OrderView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderRepository orders;

    @Tool(description = """
            주문 상태를 조회한다.
            사용자가 주문번호를 명확히 제공했을 때만 호출한다.
            """)
    public OrderView getOrder(
            @ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
            ToolContext context) {
        String userId = (String) context.getContext().get("userId");

        return orders.findByIdAndOwnerId(orderId, userId)
                .map(order -> {
                    log.info(
                            "tool=getOrder userId={} orderId={} result=FOUND",
                            userId,
                            orderId);
                    return OrderView.from(order);
                })
                .orElseThrow(() -> {
                    log.warn(
                            "tool=getOrder userId={} orderId={} result=NOT_FOUND",
                            userId,
                            orderId);
                    return new IllegalArgumentException(
                            "주문을 찾을 수 없습니다.");
                });
    }
}
