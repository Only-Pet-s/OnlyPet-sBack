package com.op.back.lecture.dto;

import java.util.List;

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
