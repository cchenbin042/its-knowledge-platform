package com.its.platform.rag.splitter;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResult {
    private String text;
    private String heading;
    private int startIndex;
    private int endIndex;
}