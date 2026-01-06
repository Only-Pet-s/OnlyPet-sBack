package com.op.back.lecture.dto;

import java.time.Instant;

public record LectureReviewResponse(
        String uid,
        String nickname,
        double rating,
        String content,
        Instant createdAt,
        Instant updatedAt,
        boolean mine
) {}
