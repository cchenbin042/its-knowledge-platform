package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.common.result.ResultCode;
import com.its.platform.core.ingestion.IngestionService;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;

    @PostMapping("/upload")
    public Result<DocumentEntity> upload(@RequestParam("file") MultipartFile file) throws IOException {
        DocumentEntity document = ingestionService.ingest(file);
        return Result.success(document);
    }

    @GetMapping
    public Result<List<DocumentEntity>> list() {
        List<DocumentEntity> documents = documentRepository.findAll();
        return Result.success(documents);
    }

    @GetMapping("/{title}")
    public Result<DocumentEntity> get(@PathVariable String title) {
        return documentRepository.findByTitle(title)
            .map(Result::success)
            .orElse(Result.fail(ResultCode.NOT_FOUND));
    }

    @DeleteMapping("/{title}")
    public Result<Void> delete(@PathVariable String title) {
        documentRepository.deleteByTitle(title);
        return Result.success();
    }
}