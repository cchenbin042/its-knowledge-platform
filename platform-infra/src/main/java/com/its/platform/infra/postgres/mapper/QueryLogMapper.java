package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.QueryLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface QueryLogMapper extends BaseMapper<QueryLogEntity> {

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM query_logs " +
            "WHERE created_at >= #{since} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<Map<String, Object>> countByDate(LocalDateTime since);

    @Select("SELECT AVG(duration_ms) FROM query_logs WHERE created_at >= #{since}")
    Double averageDuration(LocalDateTime since);

    @Select("SELECT COUNT(*) FROM query_logs WHERE cache_hit = true AND created_at >= #{since}")
    Long countCacheHits(LocalDateTime since);

    @Select("SELECT COUNT(*) as total_queries, " +
            "AVG(duration_ms) as avg_duration, " +
            "AVG(source_count) as avg_sources " +
            "FROM query_logs WHERE created_at >= #{since}")
    Map<String, Object> aggregateStats(LocalDateTime since);

    @Select("SELECT question, COUNT(*) as count " +
            "FROM query_logs " +
            "WHERE created_at >= #{since} " +
            "GROUP BY question " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> topQuestions(LocalDateTime since, int limit);
}