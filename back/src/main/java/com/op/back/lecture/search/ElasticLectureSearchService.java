package com.op.back.lecture.search;

import com.op.back.lecture.service.LectureSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticLectureSearchService implements LectureSearchService {

    private final ElasticsearchOperations operations;

    @Override
    public List<String> searchLectureIds(String keyword, List<String> tags, String category,int limit, int offset) {
        // 기본 조건: 승인 + 공개
        Criteria criteria = new Criteria("adminApproved").is(true)
                .and("published").is(true);

        // 키워드 검색 (title OR description)
        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and(
                    new Criteria("title").contains(keyword)
                            .or("description").contains(keyword)
            );
        }

        // 태그 검색
        if (tags != null && !tags.isEmpty()) {
            criteria = criteria.and("tags").in(tags);
        }

        // 카테고리
        if (category != null && !category.isBlank()) {
            criteria = criteria.and("category").is(category);
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(offset / limit, limit));

        return operations.search(query, LectureSearchDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(LectureSearchDocument::getLectureId)
                .toList();
    }
}