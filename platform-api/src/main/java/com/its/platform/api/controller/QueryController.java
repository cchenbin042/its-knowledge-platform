package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.query.QueryOptions;
import com.its.platform.core.query.QueryResponse;
import com.its.platform.core.query.QueryService;
import com.its.platform.api.dto.QueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public Result<QueryResponse> query(@RequestBody QueryRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Result.fail(400, "Question is required");
        }

        QueryOptions options = QueryOptions.builder()
            .expandQuery(request.isExpandQuery())
            .rerank(request.isRerank())
            .topN(request.getTopN())
            .build();

        QueryResponse response = queryService.query(
            request.getQuestion(),
            request.getSessionId(),
            options
        );
        return Result.success(response);
    }
}