package com.op.back.lecture.dto;

import java.util.List;

import com.op.back.lecture.model.Lecture;

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
    public static LectureListItemResponse from(Lecture lecture) {
        return new LectureListItemResponse(
                lecture.getLectureId(),
                lecture.getTitle(),
                lecture.getThumbnailUrl(),
                lecture.getLecturerUid(),
                lecture.getLecturerName(),
                lecture.getRating(),
                lecture.getPrice(),
                lecture.getTags()
        );
    }
}