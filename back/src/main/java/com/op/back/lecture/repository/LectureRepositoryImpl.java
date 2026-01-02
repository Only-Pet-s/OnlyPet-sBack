package com.op.back.lecture.repository;

import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.op.back.lecture.model.Lecture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.op.back.lecture.model.LectureVideo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class LectureRepositoryImpl implements LectureRepository {

    private final Firestore firestore;

    @Override
    public void save(Lecture lecture) {
        try {
            firestore.collection("lectures")
                    .document(lecture.getLectureId())
                    .set(lecture)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
        try {
            return firestore.collection("lectures")
                .whereEqualTo("adminApproved", true)
                .whereEqualTo("published", true)
                .offset(offset)
                .limit(limit)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(doc -> doc.toObject(Lecture.class))
                .toList();
        }catch(Exception e) {
            throw new RuntimeException("강의 목록 조회 실패", e);
        }
    }

    @Override
    public List<Lecture> findByLecturerUidPublishedApproved(
            String uid, int limit, int offset) {
        return List.of();
    }

    @Override
    public List<LectureVideo> findVideosByLectureId(String lectureId) {
        try {
            return firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .orderBy("order")
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map((QueryDocumentSnapshot doc) -> doc.toObject(LectureVideo.class))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("영상 목록 조회 실패", e);
        }
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
