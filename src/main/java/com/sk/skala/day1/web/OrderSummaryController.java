package com.sk.skala.day1.web;

import com.sk.skala.day1.service.OrderSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lab1/orders")
@Tag(name = "Day1 실습: 주문 요약")
public class OrderSummaryController {

    private final OrderSummaryService service;

    public OrderSummaryController(OrderSummaryService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}/summary")
    @Operation(
            summary = "주문 한 문장 요약",
            description = "본인 주문만 요약된다. 모델을 호출하므로 비용이 발생한다.")
    public SummaryResponse summary(
            @Parameter(description = "주문번호", example = "12345")
            @PathVariable String orderId,
            @Parameter(description = "조회 주체", example = "user1")
            @RequestParam String userId) {
        return service.summarize(orderId, userId);
    }
}
