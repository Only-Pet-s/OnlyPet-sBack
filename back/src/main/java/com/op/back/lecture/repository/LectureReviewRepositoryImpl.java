package com.op.back.lecture.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.LectureReview;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LectureReviewRepositoryImpl implements LectureReviewRepository {

    private final Firestore firestore;

    private DocumentReference lectureDoc(String lectureId) {
        return firestore.collection("lectures").document(lectureId);
    }

    private DocumentReference lectureReviewDoc(String lectureId, String uid) {
        return lectureDoc(lectureId).collection("reviews").document(uid);
    }

    private DocumentReference userReviewDoc(String uid, String lectureId) {
        return firestore.collection("users").document(uid)
                .collection("lecture_reviews").document(lectureId);
    }

    @Override
    public boolean existsReview(String lectureId, String uid) {
        try {
            return lectureReviewDoc(lectureId, uid).get().get().exists();
        } catch (Exception e) {
            throw new RuntimeException("리뷰 존재 여부 확인 실패", e);
        }
    }

    @Override
    public Optional<LectureReview> findReview(String lectureId, String uid) {
        try {
            DocumentSnapshot doc = lectureReviewDoc(lectureId, uid).get().get();
            if (!doc.exists()) return Optional.empty();
            return Optional.ofNullable(doc.toObject(LectureReview.class));
        } catch (Exception e) {
            throw new RuntimeException("리뷰 조회 실패", e);
        }
    }

    @Override
    public List<LectureReview> findReviews(String lectureId, int limit, int offset) {
        try {
            return lectureDoc(lectureId)
                    .collection("reviews")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .offset(offset)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(d -> d.toObject(LectureReview.class))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("리뷰 목록 조회 실패", e);
        }
    }

    @Override
    public void createReview(String lectureId, String uid, LectureReview review, String lectureTitle) {
        try {
            ApiFuture<Void> future = firestore.runTransaction(tx -> {
                DocumentReference lectureRef = lectureDoc(lectureId);
                DocumentReference lecReviewRef = lectureReviewDoc(lectureId, uid);
                DocumentReference userReviewRef = userReviewDoc(uid, lectureId);

                DocumentSnapshot lectureSnap = tx.get(lectureRef).get();
                if (!lectureSnap.exists()) {
                    throw new IllegalArgumentException("강의를 찾을 수 없습니다.");
                }

                DocumentSnapshot existing = tx.get(lecReviewRef).get();
                if (existing.exists()) {
                    throw new IllegalStateException("이미 리뷰를 작성했습니다.");
                }

                Lecture lecture = lectureSnap.toObject(Lecture.class);
                double oldAvg = lecture != null ? lecture.getRating() : 0.0;
                int oldCount = lecture != null ? lecture.getReviewCount() : 0;

                double newAvg = (oldAvg * oldCount + review.getRating()) / (oldCount + 1);
                int newCount = oldCount + 1;

                tx.set(lecReviewRef, review);
                tx.set(userReviewRef, new UserLectureReview(
                        lectureId,
                        lectureTitle,
                        review.getRating(),
                        review.getContent(),
                        review.getCreatedAt(),
                        review.getUpdatedAt()
                ));

                tx.update(lectureRef, "rating", newAvg, "reviewCount", newCount);
                return null;
            });

            future.get();
        } catch (ExecutionException e) {
            // unwrap common causes
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("리뷰 작성 실패", e);
        } catch (Exception e) {
            throw new RuntimeException("리뷰 작성 실패", e);
        }
    }

    @Override
    public void updateReview(String lectureId, String uid, LectureReview review, String lectureTitle) {
        try {
            ApiFuture<Void> future = firestore.runTransaction(tx -> {
                DocumentReference lectureRef = lectureDoc(lectureId);
                DocumentReference lecReviewRef = lectureReviewDoc(lectureId, uid);
                DocumentReference userReviewRef = userReviewDoc(uid, lectureId);

                DocumentSnapshot lectureSnap = tx.get(lectureRef).get();
                if (!lectureSnap.exists()) {
                    throw new IllegalArgumentException("강의를 찾을 수 없습니다.");
                }

                DocumentSnapshot existingSnap = tx.get(lecReviewRef).get();
                if (!existingSnap.exists()) {
                    throw new IllegalStateException("작성한 리뷰가 없습니다.");
                }

                Lecture lecture = lectureSnap.toObject(Lecture.class);
                double oldAvg = lecture != null ? lecture.getRating() : 0.0;
                int count = lecture != null ? lecture.getReviewCount() : 0;

                LectureReview existing = existingSnap.toObject(LectureReview.class);
                double oldScore = existing != null ? existing.getRating() : 0.0;

                // count는 1 이상이어야 정상
                double newAvg = count <= 1
                        ? review.getRating()
                        : oldAvg + ((review.getRating() - oldScore) / count);

                // createdAt은 기존 값 유지 (갱신은 updatedAt만)
                if (existing != null && existing.getCreatedAt() != null) {
                    review.setCreatedAt(existing.getCreatedAt());
                }

                tx.set(lecReviewRef, review, SetOptions.merge());
                tx.set(userReviewRef, new UserLectureReview(
                        lectureId,
                        lectureTitle,
                        review.getRating(),
                        review.getContent(),
                        review.getCreatedAt(),
                        review.getUpdatedAt()
                ), SetOptions.merge());

                tx.update(lectureRef, "rating", newAvg);
                return null;
            });

            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("리뷰 수정 실패", e);
        } catch (Exception e) {
            throw new RuntimeException("리뷰 수정 실패", e);
        }
    }

    @Override
    public void deleteReview(String lectureId, String uid) {
        try {
            ApiFuture<Void> future = firestore.runTransaction(tx -> {
                DocumentReference lectureRef = lectureDoc(lectureId);
                DocumentReference lecReviewRef = lectureReviewDoc(lectureId, uid);
                DocumentReference userReviewRef = userReviewDoc(uid, lectureId);

                DocumentSnapshot lectureSnap = tx.get(lectureRef).get();
                if (!lectureSnap.exists()) {
                    throw new IllegalArgumentException("강의를 찾을 수 없습니다.");
                }

                DocumentSnapshot reviewSnap = tx.get(lecReviewRef).get();
                if (!reviewSnap.exists()) {
                    throw new IllegalStateException("작성한 리뷰가 없습니다.");
                }

                Lecture lecture = lectureSnap.toObject(Lecture.class);
                double oldAvg = lecture != null ? lecture.getRating() : 0.0;
                int oldCount = lecture != null ? lecture.getReviewCount() : 0;

                LectureReview existing = reviewSnap.toObject(LectureReview.class);
                double score = existing != null ? existing.getRating() : 0.0;

                int newCount = Math.max(0, oldCount - 1);
                double newAvg;
                if (newCount == 0) {
                    newAvg = 0.0;
                } else {
                    // remove this score from average
                    newAvg = ((oldAvg * oldCount) - score) / newCount;
                }

                tx.delete(lecReviewRef);
                tx.delete(userReviewRef);
                tx.update(lectureRef, "rating", newAvg, "reviewCount", newCount);
                return null;
            });

            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("리뷰 삭제 실패", e);
        } catch (Exception e) {
            throw new RuntimeException("리뷰 삭제 실패", e);
        }
    }

    @Override
    public List<LectureReview> findMyReviews(String uid) {
        try {
            return firestore.collection("users")
                    .document(uid)
                    .collection("lecture_reviews")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(LectureReview.class))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("내 강의 리뷰 조회 실패", e);
        }
    }

    // users/{uid}/lecture_reviews/{lectureId} 저장용 내부 모델
    private static class UserLectureReview {
        private String lectureId;
        private String lectureTitle;
        private double rating;
        private String content;
        private Timestamp createdAt;
        private Timestamp updatedAt;

        @SuppressWarnings("unused")
        public UserLectureReview() {}

        public UserLectureReview(String lectureId, String lectureTitle, double rating, String content,
                                 Timestamp createdAt, Timestamp updatedAt) {
            this.lectureId = lectureId;
            this.lectureTitle = lectureTitle;
            this.rating = rating;
            this.content = content;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getLectureId() { return lectureId; }
        public String getLectureTitle() { return lectureTitle; }
        public double getRating() { return rating; }
        public String getContent() { return content; }
        public Timestamp getCreatedAt() { return createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
    }

}
