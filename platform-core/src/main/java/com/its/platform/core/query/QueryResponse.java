package com.its.platform.core.query;

import com.its.platform.rag.retriever.RetrievalResult;
import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class QueryResponse {
    private String question;
    private String answer;
    private List<RetrievalResult> sources;
}