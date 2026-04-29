package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
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
        QueryResponse response = queryService.query(request.getQuestion());
        return Result.success(response);
    }
}