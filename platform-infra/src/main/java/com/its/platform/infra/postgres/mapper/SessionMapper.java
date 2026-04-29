package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.SessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
}