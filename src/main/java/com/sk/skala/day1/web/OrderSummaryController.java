package com.sk.skala.day1.web;

import java.util.List;

import com.sk.skala.day1.service.OrderSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;


@RestController
@Tag(name = "Day1 실습: 주문 요약")
@RequiredArgsConstructor
public class OrderSummaryController {

    private final OrderSummaryService service;
    private final VectorStore vectorStore;
    

    @GetMapping(value = "/lab1/orders/{orderId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "주문 한 문장 요약",
            description = "본인 주문만 요약된다. 모델을 호출하므로 비용이 발생한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요약 성공 또는 AI 실패 후 폴백 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "필수 요청 파라미터 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "없는 주문이거나 다른 사용자의 주문",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "처리되지 않은 서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public SummaryResponse summary(
            @Parameter(description = "주문번호", example = "12345")
            @PathVariable String orderId,
            @Parameter(description = "조회 주체", example = "user1")
            @RequestParam String userId) {
        return service.summarize(orderId, userId);
    }

    @GetMapping("/lab2/retrieve")
    public List<Chunk> retrieve(@RequestParam String q, @RequestParam(defaultValue = "4") int topK){
        return vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(topK)
        .similarityThreshold(0.5)
        .build())
        .stream()
        .map(d -> new Chunk(String.valueOf(d.getMetadata().get("source"))
        ,d.getScore(), snippet(d.getText(), 120)))
        .toList();
    }

    private static String snippet(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }
}
