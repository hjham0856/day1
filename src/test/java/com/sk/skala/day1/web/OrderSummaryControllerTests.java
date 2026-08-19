package com.sk.skala.day1.web;

import com.sk.skala.day1.service.OrderSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderSummaryController.class)
@Import(Lab1ExceptionHandler.class)
class OrderSummaryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderSummaryService service;

    @Test
    void 정상_요청은_주문번호와_요약을_반환한다() throws Exception {
        when(service.summarize("12345", "user1"))
                .thenReturn(new SummaryResponse("12345", "무선 이어폰 · 배송 중"));

        mockMvc.perform(get("/lab1/orders/12345/summary").param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("12345"))
                .andExpect(jsonPath("$.summary").value("무선 이어폰 · 배송 중"));
    }

    @Test
    void 다른_사용자의_주문은_404로_숨긴다() throws Exception {
        when(service.summarize("99999", "user1"))
                .thenThrow(new OrderNotFoundException("99999", "user1"));

        mockMvc.perform(get("/lab1/orders/99999/summary").param("userId", "user1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("주문을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.traceId").isEmpty());
    }

    @Test
    void userId가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/lab1/orders/12345/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("필수 요청 파라미터가 누락되었습니다: userId"))
                .andExpect(jsonPath("$.traceId").isEmpty());
    }

    @Test
    void 처리되지_않은_오류는_503과_추적ID를_반환한다() throws Exception {
        when(service.summarize("12345", "user1"))
                .thenThrow(new IllegalStateException("internal detail"));

        mockMvc.perform(get("/lab1/orders/12345/summary").param("userId", "user1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("요약을 만들지 못했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{8}")));
    }
}
