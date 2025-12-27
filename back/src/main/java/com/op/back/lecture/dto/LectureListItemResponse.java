package com.op.back.lecture.dto;

import java.util.List;

//강의 테마 목록 조회
public record LectureListItemResponse(
    String lectureId,
    String title,
    String thumbnailUrl,
    String lecturerUid,
    String lecturerName,
    double rating,
    int price,
    List<String> tags
) {
}
