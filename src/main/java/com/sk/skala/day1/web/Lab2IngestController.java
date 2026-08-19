package com.sk.skala.day1.web;

import java.io.IOException;
import java.util.List;

import com.sk.skala.day1.service.Lab2IngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Day1 실습: 문서 적재")
@RequiredArgsConstructor
public class Lab2IngestController {

    private final Lab2IngestService ingest;

    @PostMapping(value = "/lab2/ingest", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "정책 문서 적재",
            description = "lab2-docs/*.md를 청킹해 벡터 스토어에 넣는다. "
                    + "임베딩 모델을 호출하므로 비용이 발생한다. "
                    + "스토어가 인메모리라 앱을 재시작하면 다시 호출해야 한다.")
    public List<IngestResult> ingest() throws IOException {
        return ingest.ingestAll();
    }
}
