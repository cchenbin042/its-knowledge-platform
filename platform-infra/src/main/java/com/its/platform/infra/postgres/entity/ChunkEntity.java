package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.its.platform.infra.pgvector.VectorTypeHandler;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档分块实体，对应 chunks 表
 * 支持向量存储和全文检索
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chunks", autoResultMap = true)
public class ChunkEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    /**
     * 向量嵌入，1024 维 (BGE-M3)
     * PostgreSQL vector 类型通过自定义 TypeHandler 处理
     */
    @TableField(typeHandler = VectorTypeHandler.class)
    private float[] embedding;

    private LocalDateTime createdAt;
}