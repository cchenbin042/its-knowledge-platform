package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.FeedbackEntity;
import com.its.platform.infra.postgres.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedbackRepository {

    private final FeedbackMapper feedbackMapper;

    public FeedbackEntity save(FeedbackEntity feedback) {
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public FeedbackEntity findById(Long id) {
        return feedbackMapper.selectById(id);
    }

    public List<FeedbackEntity> findBySessionId(String sessionId) {
        LambdaQueryWrapper<FeedbackEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackEntity::getSessionId, sessionId);
        return feedbackMapper.selectList(wrapper);
    }

    public List<FeedbackEntity> findByCreatedAtAfter(LocalDateTime since) {
        LambdaQueryWrapper<FeedbackEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(FeedbackEntity::getCreatedAt, since);
        return feedbackMapper.selectList(wrapper);
    }

    public List<FeedbackEntity> findAll() {
        return feedbackMapper.selectList(null);
    }

    public void deleteById(Long id) {
        feedbackMapper.deleteById(id);
    }
}