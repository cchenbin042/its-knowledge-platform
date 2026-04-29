package com.its.platform.core.query;

import com.its.platform.rag.retriever.CompositeRetriever;
import com.its.platform.rag.retriever.RetrievalResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final CompositeRetriever compositeRetriever;
    private final ChatLanguageModel chatModel;

    public QueryResponse query(String question) {
        // Retrieve
        List<RetrievalResult> results = compositeRetriever.retrieve(question);

        // Take top N to build context
        List<RetrievalResult> topResults = results.stream()
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(8)
            .collect(Collectors.toList());

        String context = buildContext(topResults);

        // Generate answer
        String prompt = buildPrompt(question, context);
        String answer = chatModel.generate(prompt);

        log.info("Query completed: {} sources", topResults.size());

        return QueryResponse.builder()
            .question(question)
            .answer(answer)
            .sources(topResults)
            .build();
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
}