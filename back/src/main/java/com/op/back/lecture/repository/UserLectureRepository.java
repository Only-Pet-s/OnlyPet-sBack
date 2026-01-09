package com.op.back.lecture.repository;

import com.op.back.lecture.model.UserLecture;

import java.util.List;
import java.util.Optional;

public interface UserLectureRepository {
    void save(String uid, UserLecture userLecture);
    Optional<UserLecture> findById(String uid, String lectureId);
    List<UserLecture> findAll(String uid);
    void update(String uid, String lectureId, java.util.Map<String,Object> updates);
}
