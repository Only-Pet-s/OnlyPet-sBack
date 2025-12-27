package com.op.back.lecture.repository;

import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.op.back.lecture.model.Lecture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LectureRepositoryImpl implements LectureRepository {

    private final Firestore firestore;

    @Override
    public void save(Lecture lecture) {
        firestore.collection("lectures")
                .document(lecture.getLectureId())
                .set(lecture)
                .get();
    }

    @Override
    public Optional<Lecture> findById(String lectureId) {
        try {
            var doc = firestore.collection("lectures")
                    .document(lectureId)
                    .get()
                    .get();
            return doc.exists()
                    ? Optional.of(doc.toObject(Lecture.class))
                    : Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Lecture> findAllPublishedApproved(int limit, int offset) {
        // 지금은 빈 구현 or TODO로 둬도 됨
        //TODO: published == true && adminApproved == true 조건으로 조회
        return List.of();
    }

    @Override
    public List<Lecture> findByLecturerUidPublishedApproved(
            String uid, int limit, int offset) {
        return List.of();
    }


    @Override
    public void saveVideo(String lectureId, LectureVideo video) {
        try {
            firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .document(video.getVideoId())
                    .set(video)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("영상 저장 실패", e);
        }
    }


    @Override
    public void incrementVideoCount(String lectureId) {
        try {
            firestore.collection("lectures")
                    .document(lectureId)
                    .update("videoCount", FieldValue.increment(1))
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("videoCount 증가 실패", e);
        }
    }
}
