package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.QueryLogEntity;
import com.its.platform.infra.postgres.mapper.QueryLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class QueryLogRepository {

    private final QueryLogMapper queryLogMapper;

    public QueryLogEntity save(QueryLogEntity log) {
        queryLogMapper.insert(log);
        return log;
    }

    public List<QueryLogEntity> findByCreatedAtAfter(LocalDateTime since) {
        LambdaQueryWrapper<QueryLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(QueryLogEntity::getCreatedAt, since);
        return queryLogMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> countByDate(LocalDateTime since) {
        return queryLogMapper.countByDate(since);
    }

    public Double averageDuration(LocalDateTime since) {
        return queryLogMapper.averageDuration(since);
    }

    public Long countCacheHits(LocalDateTime since) {
        return queryLogMapper.countCacheHits(since);
    }

    public Map<String, Object> aggregateStats(LocalDateTime since) {
        return queryLogMapper.aggregateStats(since);
    }

    public List<Map<String, Object>> topQuestions(LocalDateTime since, int limit) {
        return queryLogMapper.topQuestions(since, limit);
    }
}