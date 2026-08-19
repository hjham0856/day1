package com.sk.skala.day1.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.sk.skala.day1.web.IngestResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Lab2IngestService {
    private final VectorStore vectorStore;

    public IngestResult ingest(Resource doc, String source, String version) {
        var reader = new TextReader(doc);

        reader.getCustomMetadata().put("source", source);
        reader.getCustomMetadata().put("version", version);

        var splitter = TokenTextSplitter.builder()
            .withChunkSize(400)
            .withMinChunkSizeChars(200)
            .build();
        List<Document> chunks = splitter.apply(reader.get());

        vectorStore.delete(new FilterExpressionBuilder()
            .eq("source", source).build());
        vectorStore.add(chunks);

        return new IngestResult(source, chunks.size());
    }
}
