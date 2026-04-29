package com.its.platform.core.ingestion;

import com.its.platform.infra.es.EsDocumentIndexer;
import com.its.platform.infra.milvus.MilvusVectorStore;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import com.its.platform.rag.splitter.ChunkResult;
import com.its.platform.rag.splitter.MarkdownChunker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final MarkdownChunker markdownChunker;
    private final EmbeddingModel embeddingModel;
    private final MilvusVectorStore milvusVectorStore;
    private final EsDocumentIndexer esDocumentIndexer;

    public DocumentEntity ingest(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename();
        String title = filename != null ? filename.replace(".md", "") : "untitled";

        // Calculate content hash
        String contentHash = hashContent(content);

        // Dedup check
        if (documentRepository.existsByContentHash(contentHash)) {
            log.warn("Document already exists: {}", title);
            throw new RuntimeException("Document already exists");
        }

        // Chunk
        List<ChunkResult> chunks = markdownChunker.chunk(content, title);

        // Vectorize and store
        List<TextSegment> segments = chunks.stream()
            .map(c -> TextSegment.from(c.getText(),
                dev.langchain4j.data.document.Metadata.from("document_id", title)))
            .collect(Collectors.toList());

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        milvusVectorStore.addAll(embeddings, segments);

        // ES index
        esDocumentIndexer.indexDocument(title, title, content);

        // PG record
        DocumentEntity document = DocumentEntity.builder()
            .title(title)
            .contentHash(contentHash)
            .filePath(filename)
            .chunkCount(chunks.size())
            .createdAt(LocalDateTime.now())
            .build();

        documentRepository.save(document);

        log.info("Ingested document: {} with {} chunks", title, chunks.size());
        return document;
    }

    private String hashContent(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash content", e);
        }
    }
}