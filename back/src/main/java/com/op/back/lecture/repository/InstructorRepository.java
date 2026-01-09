package com.op.back.lecture.repository;

import java.util.Optional;

import com.op.back.lecture.model.Instructor;

/*
 * instructors 컬렉션 전용 Repository
 * instructors/{instructorUid}
 * instructors/{instructorUid}/students/{studentUid}  // 중복 제거된 수강자 집계용
 */
public interface InstructorRepository {
    void create(Instructor instructor);
    Optional<Instructor> findByUid(String instructorUid);

    void update(String instructorUid, java.util.Map<String, Object> updates);

    void incrementLectureCount(String instructorUid);
    void incrementTotalPurchases(String instructorUid);

    /*
     * 최초 수강 시작 시(= users/{uid}/lectures/{lectureId} 생성 시점)만 totalStudents +1
     */
    void addStudentIfNew(String instructorUid, String studentUid);

    /*
     * 강사 평점/리뷰 수는 '강의(테마) 리뷰'의 총합으로 집계.
     * lectures 컬렉션에서 lecturerUid = instructorUid 인 문서를 조회하여 계산한 뒤 저장.
     */
    void updateRatingStats(String instructorUid, double averageRating, int reviewCount);
}
