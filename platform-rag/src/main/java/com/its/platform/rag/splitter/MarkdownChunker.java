package com.its.platform.rag.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MarkdownChunker {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    private final int chunkSize = 2500;
    private final int overlap = 300;

    public List<ChunkResult> chunk(String markdownContent, String documentTitle) {
        List<ChunkResult> chunks = new ArrayList<>();

        // Split by headers
        Matcher matcher = HEADER_PATTERN.matcher(markdownContent);
        List<Integer> headerPositions = new ArrayList<>();
        List<String> headerTitles = new ArrayList<>();

        headerPositions.add(0);
        headerTitles.add(documentTitle);

        while (matcher.find()) {
            headerPositions.add(matcher.start());
            headerTitles.add(matcher.group(2).trim());
        }
        headerPositions.add(markdownContent.length());

        // Create chunks based on header positions
        for (int i = 0; i < headerPositions.size() - 1; i++) {
            int start = headerPositions.get(i);
            int end = headerPositions.get(i + 1);
            String heading = headerTitles.get(i);
            String content = markdownContent.substring(start, end).trim();

            if (content.length() > chunkSize) {
                splitLargeChunk(content, heading, chunks);
            } else if (!content.isEmpty()) {
                chunks.add(ChunkResult.builder()
                    .text(content)
                    .heading(heading)
                    .startIndex(start)
                    .endIndex(end)
                    .build());
            }
        }

        log.info("Chunked document into {} chunks", chunks.size());
        return chunks;
    }

    private void splitLargeChunk(String content, String heading, List<ChunkResult> chunks) {
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunkText = content.substring(start, end);

            chunks.add(ChunkResult.builder()
                .text(chunkText)
                .heading(heading)
                .startIndex(start)
                .endIndex(end)
                .build());

            start = end - overlap;
            if (start < 0) start = 0;
        }
    }
}