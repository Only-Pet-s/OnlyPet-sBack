package com.op.back.lecture.repository;

import com.google.cloud.firestore.Firestore;
import com.op.back.lecture.model.UserLecture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserLectureRepositoryImpl implements UserLectureRepository {

    private final Firestore firestore;

    @Override
    public void save(String uid, UserLecture userLecture) {
        try {
            firestore.collection("users")
                    .document(uid)
                    .collection("lectures")
                    .document(userLecture.getLectureId())
                    .set(userLecture)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("내 강의 저장 실패", e);
        }
    }

    @Override
    public Optional<UserLecture> findById(String uid, String lectureId) {
        try {
            var doc = firestore.collection("users")
                    .document(uid)
                    .collection("lectures")
                    .document(lectureId)
                    .get()
                    .get();
            return doc.exists() ? Optional.ofNullable(doc.toObject(UserLecture.class)) : Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("내 강의 조회 실패", e);
        }
    }

    @Override
    public List<UserLecture> findAll(String uid) {
        try {
            return firestore.collection("users")
                    .document(uid)
                    .collection("lectures")
                    .orderBy("lastWatchedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(d -> d.toObject(UserLecture.class))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("내 강의 목록 조회 실패", e);
        }
    }

    @Override
    public void update(String uid, String lectureId, Map<String, Object> updates) {
        try {
            firestore.collection("users")
                    .document(uid)
                    .collection("lectures")
                    .document(lectureId)
                    .update(updates)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("내 강의 업데이트 실패", e);
        }
    }
}
