
package com.op.back.lecture.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.op.back.lecture.dto.LectureListItemResponse;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.lecture.search.LectureSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LectureSearchService {

    private final LectureSearchRepository lectureSearchRepository;
    private final LectureRepository lectureRepository;

    public List<LectureListItemResponse> search(String q, int size) {
        return search(q, null, null, size, 0);
    }

    //확장용 로직
    public List<LectureListItemResponse> search(String keyword,List<String> tags,String category,
                int limit,int offset) {
        List<String> lectureIds =
                lectureSearchRepository.searchLectureIds(
                        keyword, tags, category, limit, offset);

        if (lectureIds.isEmpty()) {
            return List.of();
        }

        List<Lecture> lectures =
                lectureRepository.findByIds(lectureIds);

        Map<String, Lecture> map = lectures.stream()
                .collect(Collectors.toMap(Lecture::getLectureId, l -> l));

        return lectureIds.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .map(LectureListItemResponse::from)
                .toList();
    }
}
