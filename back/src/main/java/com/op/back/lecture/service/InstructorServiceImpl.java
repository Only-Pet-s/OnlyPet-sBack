package com.op.back.lecture.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.dto.InstructorUpdateRequest;
import com.op.back.lecture.model.Instructor;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.repository.InstructorRepository;
import com.op.back.lecture.repository.LectureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstructorServiceImpl implements InstructorService{
    private final InstructorRepository instructorRepository;
    private final LectureRepository lectureRepository;

    @Override
    public void registerInstructor(String instructorUid, InstructorCreateRequest req) {

        Instructor instructor = new Instructor();
        instructor.setInstructorUid(instructorUid);
        instructor.setName(req.name());
        instructor.setIntro(req.intro());
        instructor.setCareerYears(req.careerYears());
        instructor.setSpecialty(req.specialty());

        // 집계 필드 기본값
        instructor.setLectureCount(0);
        instructor.setTotalStudents(0);
        instructor.setTotalPurchases(0);
        instructor.setAverageRating(0.0);
        instructor.setReviewCount(0);

        instructor.setCreatedAt(Timestamp.now());
        instructor.setUpdatedAt(Timestamp.now());

        instructorRepository.create(instructor);
    }

    @Override
    public Instructor getInstructor(String instructorUid) {
        return instructorRepository.findByUid(instructorUid)
                .orElseThrow(() -> new IllegalArgumentException("강사를 찾을 수 없습니다."));
    }

    @Override
    public Instructor updateMyInstructor(String instructorUid, InstructorUpdateRequest req) {
        Map<String, Object> updates = new HashMap<>();
        if (req.name() != null) updates.put("name", req.name());
        if (req.profileImageUrl() != null) updates.put("profileImageUrl", req.profileImageUrl());
        if (req.intro() != null) updates.put("intro", req.intro());
        if (req.careerYears() != null) updates.put("careerYears", req.careerYears());
        if (req.specialty() != null) updates.put("specialty", req.specialty());
        updates.put("updatedAt", Timestamp.now());

        instructorRepository.update(instructorUid, updates);
        return getInstructor(instructorUid);
    }

    @Override
    public void syncRatingFromLectures(String instructorUid) {
        // 강의(테마) 리뷰 총합 기준 집계
        // NOTE: 운영에서는 pagination 필요. 지금은 limit 큰 값으로 처리.
        List<Lecture> lectures = lectureRepository.findByLecturerUidPublishedApproved(instructorUid, 1000, 0);

        double sum = 0.0;
        int totalReviews = 0;
        for (Lecture l : lectures) {
            int cnt = l.getReviewCount();
            if (cnt > 0) {
                sum += l.getRating() * cnt;
                totalReviews += cnt;
            }
        }

        double avg = totalReviews == 0 ? 0.0 : (sum / totalReviews);
        instructorRepository.updateRatingStats(instructorUid, avg, totalReviews);
    }
    
}
