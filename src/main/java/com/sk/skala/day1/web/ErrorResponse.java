package com.sk.skala.day1.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(example = "주문을 찾을 수 없습니다.") String message,
        @Schema(example = "a1b2c3d4", nullable = true) String traceId) {
}
