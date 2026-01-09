package com.op.back.lecture.dto;

public record LectureReviewUpdateRequest(
        double rating,
        String content
) {}
