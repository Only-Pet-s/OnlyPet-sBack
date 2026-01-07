package com.op.back.lecture.dto;

import java.time.Instant;

import com.op.back.lecture.model.LectureReview;

public record LectureReviewResponse(
        String uid,
        String nickname,
        double rating,
        String content,
        Instant createdAt,
        Instant updatedAt,
        boolean mine
) {
    public static LectureReviewResponse from(LectureReview review, boolean mine) {
        return new LectureReviewResponse(
                review.getUid(),
                review.getNickname(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt().toDate().toInstant(),
                review.getUpdatedAt() != null
                        ? review.getUpdatedAt().toDate().toInstant()
                        : null,
                mine
        );
    }
}