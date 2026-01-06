package com.op.back.lecture.repository;

import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.LectureVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public List<Lecture> findByIds(List<String> lectureIds) {
        if (lectureIds == null || lectureIds.isEmpty()) {
            return List.of();
        }

        try {
            // Firestore whereIn 제한: 최대 10개
            // → ES search size도 10~20으로 맞추는 게 안전
            return firestore.collection("lectures")
                    .whereIn("lectureId", lectureIds)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(Lecture.class))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("강의 ID 목록 조회 실패", e);
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
        try {
            return firestore.collection("lectures")
                    .whereEqualTo("lecturerUid", uid)
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
        } catch (Exception e) {
            throw new RuntimeException("강의자 강의 목록 조회 실패", e);
        }
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

    @Override
    public void decrementVideoCount(String lectureId) {
        try {
            firestore.collection("lectures")
                    .document(lectureId)
                    .update("videoCount", FieldValue.increment(-1))
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("videoCount 감소 실패", e);
        }
    }

    @Override
    public Optional<LectureVideo> findVideoById(String lectureId, String videoId) {
        try {
            var doc = firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .document(videoId)
                    .get()
                    .get();
            if (!doc.exists()) return Optional.empty();
            return Optional.ofNullable(doc.toObject(LectureVideo.class));
        } catch (Exception e) {
            throw new RuntimeException("영상 조회 실패", e);
        }
    }

    @Override
    public void updateVideo(String lectureId, String videoId, java.util.Map<String, Object> updates) {
        try {
            firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .document(videoId)
                    .update(updates)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("영상 수정 실패", e);
        }
    }

    @Override
    public void softDeleteVideo(String lectureId, String videoId) {
        try {
            firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .document(videoId)
                    .update("deleted", true)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("영상 삭제(soft delete) 실패", e);
        }
    }

    @Override
    public int getNextVideoOrder(String lectureId) {
        try {
            var snap = firestore.collection("lectures")
                    .document(lectureId)
                    .collection("videos")
                    .orderBy("order", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .get();

            if (snap.isEmpty()) {
                return 1;
            }

            LectureVideo last = snap.getDocuments().get(0).toObject(LectureVideo.class);
            if (last == null) {
                return 1;
            }

            return Math.max(1, last.getOrder() + 1);
        } catch (Exception e) {
            throw new RuntimeException("다음 영상 order 계산 실패", e);
        }
    }
}
