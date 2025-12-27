package com.op.back.lecture.service;

import com.op.back.lecture.dto.LectureCreateRequest;
import com.op.back.lecture.dto.LectureDetailResponse;
import com.op.back.lecture.dto.LectureListItemResponse;
import com.op.back.lecture.dto.LectureVideoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LectureService {
    String createLecture(LectureCreateRequest req, String currentUid);
    List<LectureListItemResponse> getLectures(int limit, int offset);
    LectureDetailResponse getLecture(String lectureId);
    List<LectureVideoResponse> getLectureVideos(String lectureId, String currentUid);
    List<LectureListItemResponse> getLecturesByLecturer(String lecturerUid, int limit, int offset);
    List<LectureListItemResponse> searchLectures(String keyword,List<String> tags,String category,int limit,int offset);
    void uploadVideo(String lectureId,MultipartFile video,String title,String description,int order,boolean preview,String currentUid);
}
