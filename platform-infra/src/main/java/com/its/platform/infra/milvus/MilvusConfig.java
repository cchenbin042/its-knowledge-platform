package com.its.platform.infra.milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${rag.embedding-dimension:1536}")
    private int embeddingDimension;

    @Bean
    public MilvusClientV2 milvusClient() {
        MilvusClientV2 client = new MilvusClientV2(
            io.milvus.v2.client.ConnectParam.builder()
                .uri("http://" + host + ":" + port)
                .build()
        );

        initCollections(client);
        return client;
    }

    private void initCollections(MilvusClientV2 client) {
        createCollectionIfNotExists(client, "chunks", embeddingDimension);
        createCollectionIfNotExists(client, "full_doc", embeddingDimension);
        log.info("Milvus collections initialized");
    }

    private void createCollectionIfNotExists(MilvusClientV2 client, String collectionName, int dimension) {
        HasCollectionReq hasReq = HasCollectionReq.builder()
            .collectionName(collectionName)
            .build();

        if (!client.hasCollection(hasReq)) {
            CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .dimension(dimension)
                .dataType(DataType.FloatVector)
                .build();
            client.createCollection(createReq);
            log.info("Created Milvus collection: {}", collectionName);
        }
    }
}