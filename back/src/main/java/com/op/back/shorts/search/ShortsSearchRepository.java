package com.op.back.shorts.search;

import java.util.List;

import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ShortsSearchRepository {

     private final ElasticsearchClient client;
    private static final String INDEX = "shorts-index";

    // ES 저장 (생성 시)
    public void save(ShortsDocument document) {
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(document.getId())
                    .document(document)
            );
        } catch (Exception e) {
            // ES 실패로 쇼츠 생성 실패하면 안 됨
            System.out.println("Shorts ES save failed: " + e.getMessage());
        }
    }

    // 검색
    public List<ShortsDocument> search(String keyword) {
        try {
            SearchResponse<ShortsDocument> response = client.search(
                s -> s
                    .index(INDEX)
                    .query(q -> q
                        .match(m -> m
                            .field("description")
                            .query(keyword)
                        )
                    ),
                ShortsDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Shorts search failed", e);
        }
    }
}