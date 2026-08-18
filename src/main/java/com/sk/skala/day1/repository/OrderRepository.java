package com.sk.skala.day1.repository;

import com.sk.skala.day1.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByIdAndOwnerId(String id, String ownerId);
}
