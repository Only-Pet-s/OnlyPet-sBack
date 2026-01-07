package com.op.back.lecture.repository;

import java.util.List;
import java.util.Optional;

import com.op.back.lecture.model.LectureReview;

public interface LectureReviewRepository {

    boolean existsReview(String lectureId, String uid);
    Optional<LectureReview> findReview(String lectureId, String uid);
    List<LectureReview> findReviews(String lectureId, int limit, int offset);
    void createReview(String lectureId, String uid, LectureReview review, String lectureTitle);
    void updateReview(String lectureId, String uid, LectureReview review, String lectureTitle);
    void deleteReview(String lectureId, String uid);
}
