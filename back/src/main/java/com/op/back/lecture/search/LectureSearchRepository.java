package com.op.back.lecture.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LectureSearchRepository
        extends ElasticsearchRepository<LectureSearchDocument, String> {
}
