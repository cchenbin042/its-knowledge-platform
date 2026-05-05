package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.its.platform.infra.postgres.handler.MessageListTypeHandler;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sessions", autoResultMap = true)
public class SessionEntity {
    @TableId(type = IdType.INPUT)
    private String id;

    @TableField(typeHandler = MessageListTypeHandler.class)
    private List<MessageItem> messages;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageItem {
        private String role;
        private String content;
        private LocalDateTime timestamp;
    }
}