package com.op.back.lecture.dto;

import java.util.List;

public record LectureReviewListResponse(
        double averageRating,
        int reviewCount,
        List<LectureReviewResponse> reviews
) {}
