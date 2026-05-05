package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Query log entity.
 * Records query execution details for analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("query_logs")
public class QueryLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private String question;
    private Integer durationMs;
    private Integer sourceCount;
    private Boolean cacheHit;
    private Boolean webSearchUsed;
    private LocalDateTime createdAt;
}