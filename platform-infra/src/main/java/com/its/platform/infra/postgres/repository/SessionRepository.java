package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.SessionEntity;
import com.its.platform.infra.postgres.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final SessionMapper sessionMapper;

    public SessionEntity save(SessionEntity session) {
        if (sessionMapper.selectById(session.getId()) != null) {
            sessionMapper.updateById(session);
        } else {
            sessionMapper.insert(session);
        }
        return session;
    }

    public Optional<SessionEntity> findById(String id) {
        return Optional.ofNullable(sessionMapper.selectById(id));
    }

    public List<SessionEntity> findActiveSessions() {
        LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SessionEntity::getExpiresAt, LocalDateTime.now());
        return sessionMapper.selectList(wrapper);
    }

    public void deleteExpiredSessions() {
        LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SessionEntity::getExpiresAt, LocalDateTime.now());
        sessionMapper.delete(wrapper);
    }

    public void deleteById(String id) {
        sessionMapper.deleteById(id);
    }

    public boolean existsById(String id) {
        return sessionMapper.selectById(id) != null;
    }
}