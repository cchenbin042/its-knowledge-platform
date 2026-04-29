package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final DocumentMapper documentMapper;

    public DocumentEntity save(DocumentEntity document) {
        documentMapper.insert(document);
        return document;
    }

    public Optional<DocumentEntity> findById(Long id) {
        return Optional.ofNullable(documentMapper.selectById(id));
    }

    public Optional<DocumentEntity> findByTitle(String title) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getTitle, title);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    public Optional<DocumentEntity> findByContentHash(String contentHash) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getContentHash, contentHash);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    public List<DocumentEntity> findAll() {
        return documentMapper.selectList(null);
    }

    public void deleteByTitle(String title) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getTitle, title);
        documentMapper.delete(wrapper);
    }

    public boolean existsByContentHash(String contentHash) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getContentHash, contentHash);
        return documentMapper.selectCount(wrapper) > 0;
    }
}