package com.op.back.lecture.service;

import java.util.List;

import com.op.back.lecture.dto.LectureReviewCreateRequest;
import com.op.back.lecture.dto.LectureReviewListResponse;
import com.op.back.lecture.dto.LectureReviewResponse;
import com.op.back.lecture.dto.LectureReviewUpdateRequest;

public interface LectureReviewService {

    LectureReviewResponse create(String lectureId, LectureReviewCreateRequest req, String currentUid);
    LectureReviewListResponse list(String lectureId, int limit, int offset, String currentUid);
    LectureReviewResponse getMine(String lectureId, String currentUid);
    LectureReviewResponse update(String lectureId, LectureReviewUpdateRequest req, String currentUid);
    void delete(String lectureId, String currentUid);
    List<LectureReviewResponse> getMyReviews(String uid);
}
