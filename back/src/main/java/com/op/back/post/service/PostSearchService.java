package com.op.back.post.service;

import com.op.back.post.dto.PostResponse;
import com.op.back.post.search.PostDocument;
import com.op.back.post.search.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Elasticsearch 전용 서비스.
 * PostService에서 ES 관련 코드만 분리(동작 동일).
 */
@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final PostSearchRepository postSearchRepository;
    private final PostMapperService postMapperService;

    public void upsert(PostDocument doc) {
        postSearchRepository.save(doc);
    }

    public void delete(String postId) {
        postSearchRepository.delete(postId);
    }

    public List<PostResponse> search(String q) {
        return postSearchRepository.search(q).stream()
                .map(postMapperService::toSearchResponse)
                .toList();
    }
}
