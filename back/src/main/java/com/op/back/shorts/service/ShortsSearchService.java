package com.op.back.shorts.service;

import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.search.ShortsDocument;
import com.op.back.shorts.search.ShortsSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Elasticsearch 전용 서비스 */
@Service
@RequiredArgsConstructor
public class ShortsSearchService {

    private final ShortsSearchRepository shortsSearchRepository;
    private final ShortsMapperService shortsMapperService;

    public void upsert(ShortsDocument doc) {
        shortsSearchRepository.save(doc);
    }

    public void delete(String shortsId) {
        shortsSearchRepository.delete(shortsId);
    }

    public List<ShortsResponse> search(String q) {
        return shortsSearchRepository.search(q).stream()
                .map(shortsMapperService::toSearchResponse)
                .toList();
    }
}
