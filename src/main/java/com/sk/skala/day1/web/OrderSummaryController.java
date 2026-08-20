package com.sk.skala.day1.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sk.skala.day1.service.OrderSummaryService;
import com.sk.skala.day1.tool.OrderTools;

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

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@Tag(name = "Day1 실습: 주문 요약")
@RequiredArgsConstructor
public class OrderSummaryController {

    private static final double MIN_SCORE = 0.5;

    private final OrderSummaryService service;
    private final VectorStore vectorStore;
    private final OrderTools orderTools;

    @Qualifier("lab2ChatClient")
    private final ChatClient chatClient;


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
        return search(q, topK).stream()
                .map(d -> new Chunk(sourceOf(d), d.getScore(), snippet(d.getText(), 120)))
                .toList();
    }

    private List<Document> search(String q, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(q)
                .topK(topK)
                .similarityThreshold(MIN_SCORE)
                .build());
    }

    private static String sourceOf(Document d) {
        return String.valueOf(d.getMetadata().get("source"));
    }

    private static String snippet(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.strip().replaceAll("\\s+", " ");
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }

    @PostMapping("/lab2/ask")
    public AnswerDto ask(@RequestParam String question, @RequestParam String userId) {
        // 근거는 미리보기(snippet)가 아니라 청크 전문을 넘긴다.
        List<Document> docs = search(question, 4);
        if (docs.isEmpty()) {
            return AnswerDto.unknown();
        }
        String context = docs.stream()
                .map(d -> "- source: " + sourceOf(d) + "\n  내용: " + d.getText().strip())
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .tools(orderTools)
                .toolContext(Map.of("userId", userId))
                .system("""
                        아래 [근거]만 사용해 한국어로 답한다. 추측하지 않는다.
                        근거에 있는 숫자와 표현은 원문 그대로 인용한다.
                        sources에는 답변에 실제로 사용한 근거의 source 값만 담는다.
                        답할 수 없었다면 sources는 빈 배열로 둔다.
                        grounded는 근거로 답했으면 true, 아니면 false로 한다.
                        """)
                .user(u -> u.text("[근거]\n{context}\n\n[질문] {question}")
                        .param("context", context)
                        .param("question", question))
                .call()
                .entity(AnswerDto.class);
    }

    public record AnswerDto(String answer, List<String> sources, boolean grounded) {
        static AnswerDto unknown() { return new AnswerDto("문서에 없음", List.of(), false); }
    }
}
