package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.common.result.ResultCode;
import com.its.platform.core.ingestion.IngestionService;
import com.its.platform.infra.postgres.entity.ChunkEntity;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.ChunkRepository;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    /**
     * Upload single document.
     */
    @PostMapping("/upload")
    public Result<DocumentEntity> upload(@RequestParam("file") MultipartFile file) throws IOException {
        DocumentEntity document = ingestionService.ingest(file);
        return Result.success(document);
    }

    /**
     * Batch upload documents.
     */
    @PostMapping("/upload/batch")
    public Result<List<DocumentEntity>> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        List<DocumentEntity> documents = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                DocumentEntity doc = ingestionService.ingest(file);
                documents.add(doc);
            } catch (Exception e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
                log.warn("Failed to upload file: {}", file.getOriginalFilename(), e);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Batch upload completed with {} errors", errors.size());
        }

        return Result.success(documents);
    }

    /**
     * List all documents.
     */
    @GetMapping
    public Result<List<DocumentEntity>> list() {
        List<DocumentEntity> documents = documentRepository.findAll();
        return Result.success(documents);
    }

    /**
     * Get document details by title.
     */
    @GetMapping("/{title}")
    public Result<DocumentEntity> get(@PathVariable String title) {
        return documentRepository.findByTitle(title)
            .map(Result::success)
            .orElse(Result.fail(ResultCode.NOT_FOUND));
    }

    /**
     * Get document content preview (chunks).
     */
    @GetMapping("/{title}/preview")
    public Result<DocumentPreview> preview(@PathVariable String title) {
        DocumentEntity document = documentRepository.findByTitle(title).orElse(null);
        if (document == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        List<ChunkEntity> chunks = chunkRepository.findByDocumentId(document.getId());

        // Build preview with first N chunks content
        StringBuilder contentPreview = new StringBuilder();
        int previewLimit = Math.min(5, chunks.size());
        for (int i = 0; i < previewLimit; i++) {
            contentPreview.append(chunks.get(i).getContent()).append("\n\n");
        }

        DocumentPreview preview = new DocumentPreview();
        preview.setDocument(document);
        preview.setTotalChunks(chunks.size());
        preview.setContentPreview(contentPreview.toString());
        preview.setChunks(chunks.subList(0, previewLimit));

        return Result.success(preview);
    }

    /**
     * Update/re-ingest document (overwrite existing).
     */
    @PostMapping("/{title}/update")
    public Result<DocumentEntity> update(
            @PathVariable String title,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Delete existing document first
        ingestionService.deleteDocument(title);

        // Ingest new version
        DocumentEntity document = ingestionService.ingest(file);
        return Result.success(document);
    }

    /**
     * Delete document by title.
     */
    @DeleteMapping("/{title}")
    public Result<Void> delete(@PathVariable String title) {
        ingestionService.deleteDocument(title);
        return Result.success();
    }

    /**
     * Document preview response.
     */
    @lombok.Data
    public static class DocumentPreview {
        private DocumentEntity document;
        private int totalChunks;
        private String contentPreview;
        private List<ChunkEntity> chunks;
    }
}