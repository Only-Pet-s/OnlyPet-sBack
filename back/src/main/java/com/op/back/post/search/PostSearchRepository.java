package com.op.back.post.search;

import java.util.List;

import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostSearchRepository {

    private final ElasticsearchClient client;
    private static final String INDEX = "post-index";

    public void save(PostDocument document) {
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(document.getId())
                    .document(document)
            );
        } catch (Exception e) {
            System.out.println("Post ES save failed: " + e.getMessage());
        }
    }

    public List<PostDocument> search(String keyword) {
        try {
            SearchResponse<PostDocument> response = client.search(
                s -> s
                    .index(INDEX)
                    .query(q -> q
                        .match(m -> m
                            .field("content")
                            .query(keyword)
                        )
                    ),
                PostDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Post search failed", e);
        }
    }

    //id기반 검색
    public List<String> searchPostIds(String keyword, int size) {
        try {
            SearchResponse<PostDocument> response = client.search(
                s -> s
                    .index(INDEX)
                    .size(size)
                    .query(q -> q
                        .multiMatch(m -> m
                            .query(keyword)
                            .fields("content", "hashtags")
                        )
                    ),
                PostDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.id())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Post search failed", e);
        }
    }

    public void delete(String postId) {
        try {
            client.delete(d -> d
                .index(INDEX)
                .id(postId)
            );
        } catch (Exception e) {
        }
    }
}
