package com.op.back.lecture.repository;

import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.LectureVideo;

import java.util.List;
import java.util.Optional;

public interface LectureRepository {
    void save(Lecture lecture);
    Optional<Lecture> findById(String lectureId);
    List<Lecture> findAllPublishedApproved(int limit, int offset);
    List<Lecture> findByLecturerUidPublishedApproved(String uid, int limit, int offset);
    List<LectureVideo> findVideosByLectureId(String lectureId);
    void saveVideo(String lectureId, LectureVideo video);
    void incrementVideoCount(String lectureId);
}
