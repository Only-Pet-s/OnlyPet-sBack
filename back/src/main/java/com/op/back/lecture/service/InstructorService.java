package com.op.back.lecture.service;


import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.dto.InstructorUpdateRequest;
import com.op.back.lecture.model.Instructor;


public interface InstructorService {
    
    void registerInstructor(String instructorUid, InstructorCreateRequest req);
    Instructor getInstructor(String instructorUid);

    //강사 본인이 자신의 프로필을 수정 (부분 수정)
    Instructor updateMyInstructor(String instructorUid, InstructorUpdateRequest req);
    
    //강사 평점/리뷰 수를 '강의 리뷰' 총합으로 재계산해서 저장
    void syncRatingFromLectures(String instructorUid);
}
