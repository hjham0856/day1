package com.sk.skala.day1.chat;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class RefundTicketService {

    private final AtomicLong sequence = new AtomicLong(1000);
    private final ConcurrentHashMap<String, TicketView> tickets = new ConcurrentHashMap<>();

    public TicketView create(String orderId, String userId, String reason) {
        String no = "RF-" + sequence.incrementAndGet();
        TicketView ticket = new TicketView(
                no,
                orderId,
                "PENDING",
                "접수되었습니다. 담당자 승인 후 처리됩니다.");
        tickets.put(no, ticket);
        return ticket;
    }

    public List<TicketView> pending() {
        return tickets.values().stream()
                .filter(ticket -> "PENDING".equals(ticket.status()))
                .toList();
    }
}
