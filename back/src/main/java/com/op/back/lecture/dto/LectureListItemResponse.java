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
        int videoCount,
        int totalDurationMinutes,
        double rating,
        int price,
        List<String> tags,
        boolean inMyLecture
) {
    public static LectureListItemResponse from(Lecture lecture) {
        return from(lecture, false);
    }

    public static LectureListItemResponse from(Lecture lecture, boolean inMyLecture) {
        return new LectureListItemResponse(
                lecture.getLectureId(),
                lecture.getTitle(),
                lecture.getThumbnailUrl(),
                lecture.getLecturerUid(),
                lecture.getLecturerName(),
                lecture.getVideoCount(),
                lecture.getTotalDurationMinutes(),
                lecture.getRating(),
                lecture.getPrice(),
                lecture.getTags(),
                inMyLecture
        );
    }
}