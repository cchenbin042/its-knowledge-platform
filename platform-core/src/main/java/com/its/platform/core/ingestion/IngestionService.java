package com.its.platform.core.ingestion;

import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import com.its.platform.infra.postgres.repository.ChunkRepository;
import com.its.platform.infra.postgres.entity.ChunkEntity;
import com.its.platform.rag.splitter.ChunkResult;
import com.its.platform.rag.splitter.MarkdownChunker;
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

    @Transactional
    public DocumentEntity ingest(MultipartFile file) throws IOException {
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

        // Save chunks with content (for full-text search)
        List<ChunkEntity> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunk = chunks.get(i);
            ChunkEntity entity = ChunkEntity.builder()
                .documentId(documentId)
                .chunkIndex(i)
                .content(chunk.getText())
                .createdAt(LocalDateTime.now())
                .build();
            chunkEntities.add(entity);
        }
        chunkRepository.saveAll(chunkEntities);

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