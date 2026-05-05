package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.ingestion.IngestionService;
import com.its.platform.crawler.CrawlerService;
import com.its.platform.crawler.CrawlerService.CrawlResult;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Crawler API controller.
 * Provides endpoints to crawl iKnow articles and ingest them.
 */
@Slf4j
@RestController
@RequestMapping("/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final IngestionService ingestionService;

    /**
     * Crawl articles from a specific path (returns markdown content).
     *
     * @param path relative path to crawl (e.g., "/category/network")
     * @param maxPages maximum pages to crawl (default 10)
     * @return list of crawled articles
     */
    @GetMapping("/crawl")
    public Result<List<CrawlResult>> crawl(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "10") int maxPages) {

        List<CrawlResult> results = crawlerService.crawl(path, maxPages);
        return Result.success(results);
    }

    /**
     * Crawl and ingest articles from a specific path.
     *
     * @param path relative path to crawl (e.g., "/category/network")
     * @param maxPages maximum pages to crawl (default 10)
     * @return list of ingested documents
     */
    @PostMapping("/ingest")
    public Result<List<DocumentEntity>> crawlAndIngest(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "10") int maxPages) {

        try {
            List<CrawlResult> results = crawlerService.crawl(path, maxPages);
            List<DocumentEntity> documents = new ArrayList<>();

            for (CrawlResult result : results) {
                try {
                    MultipartFile file = crawlerService.createMultipartFile(result);
                    DocumentEntity doc = ingestionService.ingest(file);
                    documents.add(doc);
                    log.info("Ingested crawled article: {} (ID={})", result.title(), doc.getId());
                } catch (Exception e) {
                    log.warn("Failed to ingest article: {}", result.title(), e);
                }
            }

            return Result.success(documents);
        } catch (Exception e) {
            log.error("Crawl and ingest failed", e);
            return Result.fail(500, "Crawl failed: " + e.getMessage());
        }
    }
}