package com.op.back.lecture.dto;

public record LectureReviewCreateRequest(
        double rating,
        String content
) {}
