package com.sk.skala.day1.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("eval")
class Lab2GoldenTests {

    private static final Logger log = LoggerFactory.getLogger(Lab2GoldenTests.class);

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderSummaryController service;

    @Test void 골든_세트_평가() throws Exception {
        var golden = mapper.readValue(resource("golden.json"),
                new TypeReference<List<Golden>>() {});
        int pass = 0;
        for (Golden g : golden) {
            OrderSummaryController.AnswerDto a = service.ask(g.q());
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            boolean cite = g.src() == null
                    || a.sources().stream().anyMatch(s -> s.contains(g.src()));
            if (hit && cite) { pass++; }
            else { log.warn("실패: {}\n 답변: {}\n 출처: {}", g.q(), a.answer(), a.sources()); }
        }
        log.info("통과 {}/{}", pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8); // 기준선을 코드에 박아 둠다
    }

    private InputStream resource(String name) {
        return getClass().getResourceAsStream("/lab2/" + name);
    }

    private record Golden(String q, List<String> must, String src) {
    }
}
