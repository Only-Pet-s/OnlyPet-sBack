
package com.op.back.lecture.search;

import java.util.List;

import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LectureSearchRepository {

    private static final String INDEX = "lecture-index";
    private final ElasticsearchClient client;

    public List<String> searchLectureIds(
            String keyword, List<String> tags, String category, int limit, int offset) {

        try {
            var response =
                client.search(s -> s
                    .index(INDEX)
                    .from(offset)
                    .size(limit)
                    .query(q -> q
                        .bool(b -> {
                            b.filter(f -> f.term(t ->
                                t.field("adminApproved").value(true)));
                            b.filter(f -> f.term(t ->
                                t.field("published").value(true)));

                            if (keyword != null && !keyword.isBlank()) {
                                b.must(m -> m.multiMatch(mm ->
                                    mm.fields("title", "description")
                                      .query(keyword)));
                            }

                            if (tags != null && !tags.isEmpty()) {
                                b.filter(f -> f.terms(t ->
                                    t.field("tags")
                                     .terms(v -> v.value(
                                         tags.stream()
                                             .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                             .toList()
                                     ))));
                            }

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
