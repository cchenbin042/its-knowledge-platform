package com.its.platform.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Crawler service for iKnow articles.
 * Provides crawl functionality and content conversion.
 */
@Slf4j
@Service
public class CrawlerService {

    private static final String BASE_URL = "https://iknow.huawei.com";
    private static final int TIMEOUT_MS = 30000;

    /**
     * Crawl and convert articles from a specific path.
     * Returns list of crawl results that can be further processed.
     *
     * @param path relative path to crawl
     * @param maxPages maximum pages to crawl
     * @return list of crawled articles in markdown format
     */
    public List<CrawlResult> crawl(String path, int maxPages) {
        List<CrawlResult> results = new ArrayList<>();

        try {
            String url = BASE_URL + path;
            log.info("Starting crawl from: {}", url);

            Document doc = Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

            // Find article links
            Elements articleLinks = doc.select("a[href*=/article/]");

            int count = 0;
            for (Element link : articleLinks) {
                if (count >= maxPages) break;

                String articleUrl = link.absUrl("href");
                CrawlResult result = crawlArticle(articleUrl);
                if (result != null) {
                    results.add(result);
                    count++;
                }
            }

            log.info("Crawled {} articles from {}", results.size(), url);
        } catch (IOException e) {
            log.error("Failed to crawl path: {}", path, e);
        }

        return results;
    }

    /**
     * Crawl a single article and convert to markdown.
     */
    private CrawlResult crawlArticle(String articleUrl) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

            String title = extractTitle(doc);
            String content = extractContent(doc);
            String markdown = convertToMarkdown(title, content, articleUrl);

            return new CrawlResult(title, markdown, articleUrl);
        } catch (IOException e) {
            log.warn("Failed to crawl article: {}", articleUrl, e);
            return null;
        }
    }

    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("h1.title");
        if (titleElement == null) titleElement = doc.selectFirst("h1");
        if (titleElement == null) titleElement = doc.selectFirst("title");
        return titleElement != null ? titleElement.text() : "Untitled";
    }

    private String extractContent(Document doc) {
        Element contentElement = doc.selectFirst("div.article-content");
        if (contentElement == null) contentElement = doc.selectFirst("div.content");
        if (contentElement == null) contentElement = doc.selectFirst("article");
        if (contentElement == null) contentElement = doc.selectFirst("body");

        if (contentElement != null) {
            contentElement.select("script, style, nav, header, footer").remove();
            return contentElement.text();
        }
        return "";
    }

    /**
     * Convert article content to markdown format.
     */
    private String convertToMarkdown(String title, String content, String url) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(title).append("\n\n");
        md.append(content).append("\n\n");
        md.append("---\n\n");
        md.append("**Source:** ").append(url).append("\n");
        return md.toString();
    }

    /**
     * Create a mock MultipartFile from crawl result.
     */
    public MultipartFile createMultipartFile(CrawlResult result) {
        String filename = result.title().replaceAll("[^a-zA-Z0-9]", "_") + ".md";
        byte[] bytes = result.markdown().getBytes(StandardCharsets.UTF_8);
        return new MockMultipartFile(filename, bytes);
    }

    /**
     * Crawl result containing title, markdown content, and source URL.
     */
    public record CrawlResult(String title, String markdown, String url) {}

    /**
     * Simple mock implementation of MultipartFile.
     */
    private static class MockMultipartFile implements MultipartFile {
        private final String filename;
        private final byte[] content;

        MockMultipartFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        @Override public String getName() { return filename; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return "text/markdown"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
        @Override public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}