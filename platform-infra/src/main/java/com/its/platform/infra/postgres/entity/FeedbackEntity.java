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
 * User feedback entity.
 * Stores user ratings and comments on query answers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedbacks")
public class FeedbackEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private String question;
    private String answer;
    private Integer rating;     // 1-5 scale (1=very bad, 5=very good)
    private String comment;
    private LocalDateTime createdAt;
}