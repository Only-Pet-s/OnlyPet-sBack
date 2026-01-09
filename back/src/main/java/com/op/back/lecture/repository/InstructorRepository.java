package com.op.back.lecture.repository;


import java.util.Optional;

import com.op.back.lecture.model.Instructor;

public interface InstructorRepository {
    void create(Instructor instructor);
    Optional<Instructor> findByUid(String instructorUid);
    void incrementLectureCount(String instructorUid);
}