package com.op.back.lecture.search;

import com.op.back.lecture.service.LectureSearchService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticLectureSearchService implements LectureSearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public List<String> searchLectureIds(
            String keyword, List<String> tags, String category, int limit, int offset) {

        try {
            var response =
                elasticsearchClient.search(s -> s
                    .index("lecture-index")
                    .from(offset)
                    .size(limit)
                    .query(q -> q
                        .bool(b -> {
                            // 기본 조건: 승인 + 공개
                            b.filter(f -> f.term(t ->
                                t.field("adminApproved").value(true)));
                            b.filter(f -> f.term(t ->
                                t.field("published").value(true)));

                            // 키워드 (title OR description)
                            if (keyword != null && !keyword.isBlank()) {
                                b.must(m -> m.multiMatch(mm ->
                                    mm.fields("title", "description")
                                      .query(keyword)));
                            }

                            // 태그
                            if (tags != null && !tags.isEmpty()) {
                                b.filter(f -> f.terms(t ->
                                    t.field("tags")
                                     .terms(v -> v.value(
                                         tags.stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()
                                     ))
                                ));
                            }

                            // 카테고리
                            if (category != null && !category.isBlank()) {
                                b.filter(f -> f.term(t ->
                                    t.field("category").value(category)));
                            }

                            return b;
                        })
                    ),
                    LectureSearchDocument.class
                );

            return response.hits().hits().stream()
                    .map(hit -> hit.source().getLectureId())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Lecture search failed", e);
        }
    }
}