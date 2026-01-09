package com.op.back.lecture.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.op.back.lecture.model.Instructor;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InstructorRepositoryImpl implements InstructorRepository {

    private final Firestore firestore;

    @Override
    public void create(Instructor instructor) {
        try {
            firestore.collection("instructors")
                    .document(instructor.getInstructorUid())
                    .set(instructor)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("강사 등록 실패", e);
        }
    }

    @Override
    public Optional<Instructor> findByUid(String instructorUid) {
        try {
            DocumentSnapshot doc = firestore.collection("instructors")
                    .document(instructorUid)
                    .get()
                    .get();
            if (!doc.exists()) return Optional.empty();
            return Optional.ofNullable(doc.toObject(Instructor.class));
        } catch (Exception e) {
            throw new RuntimeException("강사 조회 실패", e);
        }
    }

    @Override
    public void update(String instructorUid, Map<String, Object> updates) {
        try {
            if (updates == null || updates.isEmpty()) return;
            firestore.collection("instructors")
                    .document(instructorUid)
                    .update(updates)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("강사 수정 실패", e);
        }
    }

    @Override
    public void incrementLectureCount(String instructorUid) {
        try {
            firestore.collection("instructors")
                    .document(instructorUid)
                    .update("lectureCount", FieldValue.increment(1),
                            "updatedAt", com.google.cloud.Timestamp.now())
                    .get();
        } catch (Exception e) {
            // 강사 문서가 아직 없을 수도 있음 -> create는 별도 API에서 수행
            throw new RuntimeException("강의 수 증가 실패", e);
        }
    }

    @Override
    public void incrementTotalPurchases(String instructorUid) {
        try {
            firestore.collection("instructors")
                    .document(instructorUid)
                    .update("totalPurchases", FieldValue.increment(1),
                            "updatedAt", com.google.cloud.Timestamp.now())
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("구매 수 증가 실패", e);
        }
    }

    @Override
    public void addStudentIfNew(String instructorUid, String studentUid) {
        try {
            DocumentReference instructorRef = firestore.collection("instructors").document(instructorUid);
            DocumentReference studentRef = instructorRef.collection("students").document(studentUid);

            firestore.runTransaction((Transaction.Function<Void>) tx -> {
                DocumentSnapshot s = tx.get(studentRef).get();
                if (!s.exists()) {
                    // mark presence
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("studentUid", studentUid);
                    doc.put("createdAt", com.google.cloud.Timestamp.now());
                    tx.set(studentRef, doc);
                    tx.update(instructorRef,
                            "totalStudents", FieldValue.increment(1),
                            "updatedAt", com.google.cloud.Timestamp.now());
                }
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("수강자 집계 실패", e);
        }
    }

    @Override
    public void updateRatingStats(String instructorUid, double averageRating, int reviewCount) {
        try {
            firestore.collection("instructors")
                    .document(instructorUid)
                    .update(
                            "averageRating", averageRating,
                            "reviewCount", reviewCount,
                            "updatedAt", com.google.cloud.Timestamp.now()
                    )
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("강사 평점 집계 저장 실패", e);
        }
    }
}
