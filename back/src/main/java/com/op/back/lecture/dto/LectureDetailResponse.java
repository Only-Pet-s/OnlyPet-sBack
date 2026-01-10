package com.op.back.lecture.dto;

import java.util.List;

//강의 상세조회
public record LectureDetailResponse (
    String lectureId,
    String title,
    String description,
    String category,
    int price,
    String thumbnailUrl,
    String lecturerUid,
    String lecturerName,
    String difficulty,
    List<String> learningObjectives,
    boolean adminApproved,
    boolean published,
    List<String> tags,
    int videoCount,
    int totalDurationMinutes,
    double rating,
    int reviewCount,
    String createdAt,
    List<LectureThemeResponse> themes
){}
