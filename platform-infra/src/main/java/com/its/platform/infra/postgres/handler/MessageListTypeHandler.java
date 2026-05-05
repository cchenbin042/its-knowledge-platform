package com.its.platform.infra.postgres.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.its.platform.infra.postgres.entity.SessionEntity;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * TypeHandler for PostgreSQL JSONB type specifically for List<MessageItem>.
 */
public class MessageListTypeHandler extends BaseTypeHandler<List<SessionEntity.MessageItem>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CollectionType collectionType = TypeFactory.defaultInstance()
            .constructCollectionType(List.class, SessionEntity.MessageItem.class);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<SessionEntity.MessageItem> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize List to JSON", e);
        }
        ps.setObject(i, pgObject);
    }

    @Override
    public List<SessionEntity.MessageItem> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public List<SessionEntity.MessageItem> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public List<SessionEntity.MessageItem> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private List<SessionEntity.MessageItem> parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, collectionType);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize JSON to List<MessageItem>", e);
        }
    }
}