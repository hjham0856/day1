package com.sk.skala.day1.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sk.skala.day1.web.IngestResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Lab2IngestService {

    private static final String DOCS_PATTERN = "classpath:/lab2-docs/*.md";
    private static final String VERSION = "v1";

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resources;

    public List<IngestResult> ingestAll() throws IOException {
        List<IngestResult> results = new ArrayList<>();
        for (Resource doc : resources.getResources(DOCS_PATTERN)) {
            String filename = doc.getFilename();
            if (filename != null) {
                results.add(ingest(doc, filename.replaceFirst("\\.md$", ""), VERSION));
            }
        }
        return results;
    }

    public IngestResult ingest(Resource doc, String source, String version) {
        var reader = new TextReader(doc);
        reader.getCustomMetadata().put("source", source);
        reader.getCustomMetadata().put("version", version);

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(200)
                .build();
        List<Document> chunks = splitter.apply(reader.get());

        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
        vectorStore.add(chunks);
        return new IngestResult(source, chunks.size());
    }
}
