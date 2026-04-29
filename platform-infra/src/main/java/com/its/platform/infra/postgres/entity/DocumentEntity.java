package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("documents")
public class DocumentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String contentHash;
    private String filePath;
    private Integer chunkCount;
    private LocalDateTime createdAt;
}