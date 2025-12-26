package com.op.back.lecture.service;

import com.op.back.lecture.dto.LectureCreateRequest;
import com.op.back.lecture.dto.LectureDetailResponse;
import com.op.back.lecture.dto.LectureListItemResponse;

import java.util.List;

public interface LectureService {
    String createLecture(LectureCreateRequest req, String currentUid);
    List<LectureListItemResponse> getLectures(int limit, int offset);
    LectureDetailResponse getLecture(String lectureId);
    List<LectureListItemResponse> getLecturesByLecturer(String lecturerUid, int limit, int offset);
    List<LectureListItemResponse> searchLectures(String keyword,List<String> tags,String category,int limit,int offset);
}
