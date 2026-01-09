package com.op.back.lecture.service;

import com.op.back.lecture.dto.LectureProgressUpdateRequest;
import com.op.back.lecture.dto.MyLectureSummaryResponse;

public interface MyLectureService {
    void enroll(String lectureId, String uid);
    MyLectureSummaryResponse getMyLectures(String uid);
    void updateProgress(String lectureId, String uid, LectureProgressUpdateRequest request);
}
