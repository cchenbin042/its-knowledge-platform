package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}