package com.sk.skala.day1.service;

import com.sk.skala.day1.domain.Order;
import com.sk.skala.day1.domain.OrderStatus;
import com.sk.skala.day1.repository.OrderRepository;
import com.sk.skala.day1.web.OrderNotFoundException;
import com.sk.skala.day1.web.SummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSummaryServiceTests {

    @Mock
    private OrderRepository orders;

    @Mock
    private ChatClient summaryChat;

    private ChatClient.ChatClientRequestSpec request;
    private ChatClient.CallResponseSpec response;
    private OrderSummaryService service;

    @BeforeEach
    void setUp() {
        request = mock(ChatClient.ChatClientRequestSpec.class);
        response = mock(ChatClient.CallResponseSpec.class);
        service = new OrderSummaryService(orders, summaryChat);
    }

    @Test
    void 본인_주문을_AI로_요약한다() {
        Order order = order12345();
        when(orders.findByIdAndOwnerId("12345", "user1")).thenReturn(Optional.of(order));
        prepareChatCall();
        when(response.content()).thenReturn("무선 이어폰이 배송 중이며 8월 20일 도착 예정입니다.");

        SummaryResponse result = service.summarize("12345", "user1");

        assertThat(result.orderId()).isEqualTo("12345");
        assertThat(result.summary()).isEqualTo("무선 이어폰이 배송 중이며 8월 20일 도착 예정입니다.");
    }

    @Test
    void AI_호출이_실패하면_주문정보로_폴백한다() {
        Order order = order12345();
        when(orders.findByIdAndOwnerId("12345", "user1")).thenReturn(Optional.of(order));
        prepareChatCall();
        when(response.content()).thenThrow(new IllegalStateException("model unavailable"));

        SummaryResponse result = service.summarize("12345", "user1");

        assertThat(result.summary()).isEqualTo("무선 이어폰 · 배송 중");
    }

    @Test
    void 주문이_없거나_소유자가_다르면_AI를_호출하지_않는다() {
        when(orders.findByIdAndOwnerId("99999", "user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize("99999", "user1"))
                .isInstanceOf(OrderNotFoundException.class);
        verifyNoInteractions(summaryChat);
    }

    @SuppressWarnings("unchecked")
    private void prepareChatCall() {
        when(summaryChat.prompt()).thenReturn(request);
        when(request.user(any(Consumer.class))).thenReturn(request);
        when(request.call()).thenReturn(response);
    }

    private Order order12345() {
        return new Order(
                "12345",
                "user1",
                "무선 이어폰",
                OrderStatus.SHIPPING,
                LocalDate.of(2026, 8, 20));
    }
}
