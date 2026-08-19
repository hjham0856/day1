package com.sk.skala.day1.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTests {

    @Autowired
    private OrderRepository orders;

    @Test
    void 본인_주문만_조회한다() {
        assertThat(orders.findByIdAndOwnerId("12345", "user1")).isPresent();
        assertThat(orders.findByIdAndOwnerId("12345", "user2")).isEmpty();
        assertThat(orders.findByIdAndOwnerId("99999", "user1")).isEmpty();
    }
}
