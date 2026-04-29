package com.its.platform.common.constant;

public final class RagConstants {
    public static final String COLLECTION_CHUNKS = "chunks";
    public static final String COLLECTION_FULL_DOC = "full_doc";

    public static final String ES_INDEX_NAME = "its_knowledge";

    public static final int DEFAULT_TOP_K = 8;
    public static final int DEFAULT_RRF_K = 60;

    public static final int SESSION_TTL_SECONDS = 1800; // 30 minutes

    private RagConstants() {}
}