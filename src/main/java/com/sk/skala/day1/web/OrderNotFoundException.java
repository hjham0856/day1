package com.sk.skala.day1.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId, String userId) {
        super("주문을 찾을 수 없습니다. orderId=" + orderId + ", userId=" + userId);
    }
}
