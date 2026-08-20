package com.sk.skala.day1.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.day1.chat.RefundTicketService;
import com.sk.skala.day1.chat.TicketView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lab3/admin/tickets")
@Tag(name = "Day3 실습: 환불 승인")
@RequiredArgsConstructor
public class AdminController {

    private final RefundTicketService tickets;

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 중인 환불 티켓 조회")
    public List<TicketView> pending() {
        return tickets.pending();
    }
}
