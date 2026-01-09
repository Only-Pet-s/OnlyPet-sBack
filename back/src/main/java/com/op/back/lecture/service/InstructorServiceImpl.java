package com.op.back.lecture.service;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.model.Instructor;
import com.op.back.lecture.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstructorServiceImpl implements InstructorService{
    private final InstructorRepository instructorRepository;

    public void registerInstructor(InstructorCreateRequest req,String currentUid) {
        Instructor instructor = new Instructor();
        instructor.setInstructorUid(currentUid);
        instructor.setName(req.name());
        instructor.setIntro(req.intro());
        instructor.setCareerYears(req.careerYears());
        instructor.setSpecialty(req.specialty());

        // 서버 집계 필드
        instructor.setLectureCount(0);
        instructor.setTotalStudents(0);
        instructor.setTotalPurchases(0);
        instructor.setAverageRating(0.0);
        instructor.setReviewCount(0);

        instructor.setCreatedAt(Timestamp.now());
        instructor.setUpdatedAt(Timestamp.now());

        instructorRepository.create(instructor);
    }

    public Instructor getInstructor(String instructorUid){
        return instructorRepository.findByUid(instructorUid)
                .orElseThrow(() -> new IllegalArgumentException("강사를 찾을 수 없습니다."));
    }
    
}
