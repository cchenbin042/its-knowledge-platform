package com.its.platform.core.ingestion;

import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import com.its.platform.infra.postgres.repository.ChunkRepository;
import com.its.platform.rag.splitter.ChunkResult;
import com.its.platform.rag.splitter.MarkdownChunker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final MarkdownChunker markdownChunker;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    // Maximum document size: 5MB
    private static final long MAX_DOCUMENT_SIZE = 5 * 1024 * 1024;
    // Batch size for embedding generation
    private static final int EMBEDDING_BATCH_SIZE = 20;

    @Transactional
    public DocumentEntity ingest(MultipartFile file) throws IOException {
        // Check file size
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new RuntimeException("Document too large. Maximum size is 5MB. Current: " + (file.getSize() / 1024 / 1024) + "MB");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename();
        String title = filename != null ? filename.replace(".md", "") : "untitled";

        String contentHash = hashContent(content);

        if (documentRepository.existsByContentHash(contentHash)) {
            log.warn("Document already exists: {}", title);
            throw new RuntimeException("Document already exists");
        }

        // Save document to get ID
        DocumentEntity document = DocumentEntity.builder()
            .title(title)
            .contentHash(contentHash)
            .filePath(filename)
            .chunkCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        documentRepository.save(document);
        Long documentId = document.getId();

        // Chunk
        List<ChunkResult> chunks = markdownChunker.chunk(content, title);
        log.info("Chunked document '{}' into {} chunks", title, chunks.size());

        // Generate embeddings and store in batches
        int totalChunks = chunks.size();
        int processedChunks = 0;

        for (int batchStart = 0; batchStart < totalChunks; batchStart += EMBEDDING_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + EMBEDDING_BATCH_SIZE, totalChunks);

            List<TextSegment> batchSegments = new ArrayList<>();
            for (int i = batchStart; i < batchEnd; i++) {
                ChunkResult chunk = chunks.get(i);
                Metadata metadata = Metadata.from("documentId", documentId);
                metadata.put("chunkIndex", i);
                metadata.put("title", title);
                batchSegments.add(TextSegment.from(chunk.getText(), metadata));
            }

            log.info("Generating embeddings for batch {}-{} of {} segments...", batchStart, batchEnd, totalChunks);
            List<Embedding> batchEmbeddings = embeddingModel.embedAll(batchSegments).content();

            // Store embeddings
            embeddingStore.addAll(batchEmbeddings, batchSegments);
            processedChunks += batchSegments.size();

            log.info("Processed {} / {} chunks", processedChunks, totalChunks);
        }

        // Update chunk count
        document.setChunkCount(chunks.size());
        documentRepository.update(document);

        log.info("Ingested document: {} (ID={}) with {} chunks", title, documentId, chunks.size());
        return document;
    }

    public void deleteDocument(String title) {
        DocumentEntity document = documentRepository.findByTitle(title).orElse(null);
        if (document == null) {
            log.warn("Document not found: {}", title);
            return;
        }

        chunkRepository.deleteByDocumentId(document.getId());
        documentRepository.deleteById(document.getId());

        log.info("Deleted document: {} (ID={})", title, document.getId());
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