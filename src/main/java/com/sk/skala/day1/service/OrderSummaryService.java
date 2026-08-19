package com.sk.skala.day1.service;

import com.sk.skala.day1.domain.Order;
import com.sk.skala.day1.repository.OrderRepository;
import com.sk.skala.day1.web.OrderNotFoundException;
import com.sk.skala.day1.web.SummaryResponse;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderSummaryService {

    private static final Logger log = LoggerFactory.getLogger(OrderSummaryService.class);

    private final OrderRepository orders;
    private final ChatClient summaryChat;

    public SummaryResponse summarize(String orderId, String userId) {
        Order order = orders.findByIdAndOwnerId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId, userId));

        String summary;
        try {
            summary = summaryChat.prompt()
                    .user(user -> user.text("주문번호 {id}, 상품 {item}, 상태 {status}, 도착예정일 {eta}를 "
                                    + "주어진 정보만 사용해 한국어 한 문장으로 요약해 줘. 추측하지 마.")
                            .param("id", order.getId())
                            .param("item", order.getItem())
                            .param("status", order.getStatus().label())
                            .param("eta", order.getEta()))
                    .call()
                    .content();
        }
        catch (Exception e) {
            log.warn("AI 주문 요약 실패 - 기본 정보로 폴백합니다. orderId={}", order.getId(), e);
            summary = order.getItem() + " · " + order.getStatus().label();
        }

        return new SummaryResponse(order.getId(), summary);
    }
}
