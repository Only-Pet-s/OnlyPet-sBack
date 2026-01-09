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

    /*
        ID기반 검색(ES에서 id만 뽑은 뒤, firesotre에서 재조회하는 용도)
    */
    public List<String> searchShortsIds(String keyword, int size) {
        try {
            SearchResponse<ShortsDocument> response = client.search(
                s -> s
                    .index(INDEX)
                    .size(size)
                    .query(q -> q
                        .multiMatch(m -> m
                            .query(keyword)
                            .fields("description", "hashtags")
                        )
                    ),
                ShortsDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.id())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Shorts search failed", e);
        }
    }

    public void delete(String shortsId) {
        try {
            client.delete(d -> d
                .index(INDEX)
                .id(shortsId)
            );
        } catch (Exception e) {
            // log only
        }
    }
}