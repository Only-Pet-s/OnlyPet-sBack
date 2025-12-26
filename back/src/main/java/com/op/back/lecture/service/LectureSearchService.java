package com.op.back.lecture.service;

import java.util.List;

public interface LectureSearchService {
    List<String> searchLectureIds(String keyword, List<String> tags, String category, int limit, int offset);
}
