package com.its.platform.core.query;

import com.its.platform.core.analytics.QueryLogService;
import com.its.platform.core.session.SessionService;
import com.its.platform.infra.postgres.entity.SessionEntity;
import com.its.platform.rag.expansion.QueryExpander;
import com.its.platform.rag.rerank.CrossEncoderReranker;
import com.its.platform.rag.retriever.CompositeRetriever;
import com.its.platform.rag.retriever.RetrievalResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QueryService {

    private final CompositeRetriever compositeRetriever;
    private final ChatLanguageModel chatModel;
    private final QueryCacheService queryCacheService;

    @Autowired(required = false)
    private QueryExpander queryExpander;

    @Autowired(required = false)
    private CrossEncoderReranker reranker;

    @Autowired(required = false)
    private SessionService sessionService;

    @Autowired(required = false)
    private QueryLogService queryLogService;

    public QueryService(CompositeRetriever compositeRetriever, ChatLanguageModel chatModel,
                        QueryCacheService queryCacheService) {
        this.compositeRetriever = compositeRetriever;
        this.chatModel = chatModel;
        this.queryCacheService = queryCacheService;
    }

    /**
     * Execute query with optional expansion and reranking.
     */
    public QueryResponse query(String question) {
        return query(question, null, QueryOptions.defaultOptions());
    }

    /**
     * Execute query with custom options.
     */
    public QueryResponse query(String question, QueryOptions options) {
        return query(question, null, options);
    }

    /**
     * Execute query with session support (multi-turn conversation).
     *
     * @param question user question
     * @param sessionId optional session ID for multi-turn conversation
     * @param options query options
     * @return query response with answer and sources
     */
    public QueryResponse query(String question, String sessionId, QueryOptions options) {
        long startTime = System.currentTimeMillis();

        // Step 0: Check cache (only for simple queries without session)
        if (sessionId == null && !options.isExpandQuery() && !options.isRerank()) {
            QueryResponse cachedResponse = queryCacheService.get(question);
            if (cachedResponse != null) {
                int durationMs = (int) (System.currentTimeMillis() - startTime);
                cachedResponse.setDurationMs(durationMs);

                // Log cache hit
                if (queryLogService != null) {
                    queryLogService.logQuery(null, question, durationMs,
                        cachedResponse.getSources().size(), true, false);
                }

                log.info("Cache hit for question: {} ({}ms)", question, durationMs);
                return cachedResponse;
            }
        }

        // Step 1: Get conversation history if sessionId provided
        List<SessionEntity.MessageItem> history = null;
        if (sessionId != null && sessionService != null) {
            SessionEntity session = sessionService.getSession(sessionId);
            if (session != null) {
                history = session.getMessages();
                log.info("Session found: {} with {} messages", sessionId, history.size());
            }
        }

        // Step 2: Query expansion (optional)
        List<String> expandedQueries;
        if (options.isExpandQuery() && queryExpander != null) {
            List<String> expanded = queryExpander.expand(question);
            expandedQueries = expanded != null ? expanded : List.of(question);
        } else {
            expandedQueries = List.of(question);
        }

        // Step 3: Retrieve for each expanded query
        List<RetrievalResult> allResults = new ArrayList<>();
        for (String query : expandedQueries) {
            List<RetrievalResult> results = compositeRetriever.retrieve(query);
            allResults.addAll(results);
        }

        // Deduplicate by documentId and keep top scores
        allResults = deduplicateResults(allResults);

        // Step 4: Rerank (optional)
        List<RetrievalResult> topResults;
        if (options.isRerank() && reranker != null && reranker.isEnabled()) {
            topResults = reranker.rerank(question, allResults, options.getTopN());
        } else {
            topResults = allResults.stream()
                .limit(options.getTopN())
                .collect(Collectors.toList());
        }

        String context = buildContext(topResults);

        // Step 5: Build prompt with history if available
        String prompt = buildPromptWithContext(question, context, history);

        // Step 6: Generate answer
        String answer = chatModel.generate(prompt);

        // Step 7: Save to session history if sessionId provided
        if (sessionId != null && sessionService != null) {
            sessionService.addMessage(sessionId, "user", question);
            sessionService.addMessage(sessionId, "assistant", answer);
        }

        int durationMs = (int) (System.currentTimeMillis() - startTime);

        // Step 8: Build response
        QueryResponse response = QueryResponse.builder()
            .question(question)
            .answer(answer)
            .sources(topResults)
            .sessionId(sessionId)
            .durationMs(durationMs)
            .cached(false)
            .build();

        // Step 9: Cache simple queries (without session)
        if (sessionId == null && !options.isExpandQuery() && !options.isRerank()) {
            queryCacheService.put(question, response);
        }

        // Step 10: Log query execution
        if (queryLogService != null) {
            queryLogService.logQuery(sessionId, question, durationMs, topResults.size(), false, false);
        }

        log.info("Query completed in {}ms: {} expanded queries, {} sources",
            durationMs, expandedQueries.size(), topResults.size());

        return response;
    }

    /**
     * Deduplicate results by documentId, keeping highest score for each.
     */
    private List<RetrievalResult> deduplicateResults(List<RetrievalResult> results) {
        return results.stream()
            .collect(Collectors.groupingBy(
                r -> r.getDocumentId() != null ? r.getDocumentId() : r.getContent(),
                Collectors.maxBy((a, b) -> Double.compare(a.getScore(), b.getScore()))
            ))
            .values()
            .stream()
            .flatMap(opt -> opt.stream())
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());
    }

    private String buildContext(List<RetrievalResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reference documents:\n\n");
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append(String.format("[%d] %s\n%s\n\n", i + 1, r.getTitle(), r.getContent()));
        }
        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return String.format(
            "You are an IT operations expert. Answer the user's question based on the following reference documents.\n\n" +
            "%s\n\n" +
            "User question: %s\n\n" +
            "Please provide an accurate, detailed answer based on the reference documents. If there is no relevant information, please state that.",
            context, question
        );
    }

    /**
     * Build prompt with conversation history for multi-turn support.
     */
    private String buildPromptWithContext(String question, String context, List<SessionEntity.MessageItem> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an IT operations expert. Answer the user's question based on the following reference documents.\n\n");
        sb.append(context);
        sb.append("\n");

        // Add conversation history if available
        if (history != null && !history.isEmpty()) {
            sb.append("\n=== Conversation History ===\n");
            for (SessionEntity.MessageItem msg : history) {
                sb.append(String.format("%s: %s\n", msg.getRole().toUpperCase(), msg.getContent()));
            }
            sb.append("\n");
        }

        sb.append(String.format("User question: %s\n\n", question));
        sb.append("Please provide an accurate, detailed answer based on the reference documents. ");
        sb.append("If there is relevant context from the conversation history, consider it. ");
        sb.append("If there is no relevant information, please state that.");

        return sb.toString();
    }
}