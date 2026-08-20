package com.sk.skala.day1.web;

import java.time.LocalDate;

import com.sk.skala.day1.domain.Order;

public record OrderView(
        String id,
        String item,
        String status,
        LocalDate eta
) {
    public static OrderView from(Order order) {
        return new OrderView(
                order.getId(),
                order.getItem(),
                order.getStatus().label(),
                order.getEta()
        );
    }
}