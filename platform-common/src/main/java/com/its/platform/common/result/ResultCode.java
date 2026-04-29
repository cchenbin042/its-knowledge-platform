package com.its.platform.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    DOCUMENT_NOT_FOUND(1001, "文档不存在"),
    DOCUMENT_ALREADY_EXISTS(1002, "文档已存在"),
    EMBEDDING_FAILED(1003, "向量化失败"),
    RETRIEVE_FAILED(1004, "检索失败"),
    SESSION_EXPIRED(1005, "会话已过期");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}