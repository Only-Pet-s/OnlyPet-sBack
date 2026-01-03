package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.petsitter.dto.PetsitterReviewResponseDTO;
import com.op.back.petsitter.dto.ReviewRequestDTO;
import com.op.back.petsitter.dto.ReviewUpdateDTO;
import com.op.back.petsitter.exception.ReviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PetsitterReviewService {

    private final Firestore firestore;

    public void createReview(
            String uid,
            String petsitterId,
            ReviewRequestDTO req
    ) {

        DocumentSnapshot reservation = null;

        try {
            reservation = firestore.collection("reservations")
                    .document(req.getReservationId())
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!reservation.exists()) {
            throw new ReviewException("예약이 존재하지 않습니다");
        }

        if (!uid.equals(reservation.getString("userUid"))) {
            throw new ReviewException("리뷰 작성 권한이 없습니다.");
        }

        if (!petsitterId.equals(reservation.getString("petsitterId"))) {
            throw new ReviewException("예약 정보가 일치하지 않습니다.");
        }

        if (!"RESERVED".equals(reservation.getString("reservationStatus"))
                || !"COMPLETED".equals(reservation.getString("paymentStatus"))) {
            throw new ReviewException("리뷰 작성 조건을 만족하지 않습니다.");
        }

        LocalDate date = LocalDate.parse(reservation.getString("date"));
        LocalTime endTime = LocalTime.parse(reservation.getString("endTime"));

        if (LocalDateTime.of(date, endTime).isAfter(LocalDateTime.now())) {
            throw new ReviewException("아직 서비스가 완료되지 않았습니다.");
        }

        QuerySnapshot exists = null;
        try {
            exists = firestore.collection("petsitters")
                    .document(petsitterId)
                    .collection("reviews")
                    .whereEqualTo("reservationId", req.getReservationId())
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!exists.isEmpty()) {
            throw new ReviewException("이미 리뷰를 작성했습니다.");
        }

        try {
            firestore.runTransaction(tx -> {

                DocumentReference petsitterRef =
                        firestore.collection("petsitters")
                                .document(petsitterId);

                DocumentSnapshot petsitter = tx.get(petsitterRef).get();

                String reviewId = firestore
                        .collection("petsitters")
                        .document(petsitterId)
                        .collection("reviews")
                        .document()
                        .getId();

                double currentTemp =
                        petsitter.contains("mannerTemp")
                                ? petsitter.getDouble("mannerTemp")
                                : 36.5;

                double newTemp =
                        currentTemp + ratingTemp(req.getRating());

                long reviewCount =
                        petsitter.contains("reviewCount")
                                ? petsitter.getLong("reviewCount")
                                : 0;

                double currentRating =
                        petsitter.contains("rating")
                                ? petsitter.getDouble("rating")
                                : 0.0;

                double newRating =
                        ((currentRating * reviewCount) + req.getRating())
                                / (reviewCount + 1);

                // 5. 리뷰 저장
                DocumentReference reviewRef =
                        petsitterRef.collection("reviews").document(reviewId);

                tx.set(reviewRef, Map.of(
                        "reviewId", reviewId,
                        "reservationId", req.getReservationId(),
                        "userUid", uid,
                        "rating", req.getRating(),
                        "content", req.getContent(),
                        "createdAt", Timestamp.now()
                ));

                // 추후 유저별 작성했던 리뷰 조회를 위해 users/psReviews 에도 저장
                DocumentReference userRef = firestore.collection("users").document(uid).collection("psReviews").document(reviewId);

                tx.set(userRef, Map.of(
                        "reviewId", reviewId,
                        "reservationId", req.getReservationId(),
                        "userUid", uid,
                        "rating", req.getRating(),
                        "content", req.getContent(),
                        "createdAt", Timestamp.now()
                ));

                // 6. 펫시터 통계 업데이트
                tx.update(petsitterRef,
                        "mannerTemp", newTemp,
                        "rating", Math.round(newRating * 10) / 10.0,
                        "reviewCount", reviewCount + 1
                );

                return null;
            }).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // 펫시터가 받은 리뷰 조회
    public List<PetsitterReviewResponseDTO> getPetsitterReviews(
            String petsitterId
    ) {
        QuerySnapshot snapshots = null;
        try {
            snapshots = firestore.collection("petsitters")
                    .document(petsitterId)
                    .collection("reviews")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get();
        } catch (Exception e) {
            throw new ReviewException("리뷰 조회에 실패했습니다.");
        }

        List<PetsitterReviewResponseDTO> list = new ArrayList<>();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            list.add(new PetsitterReviewResponseDTO(
                    doc.getId(),
                    doc.getString("userUid"),
                    doc.getLong("rating").intValue(),
                    doc.getString("content"),
                    doc.getTimestamp("createdAt")
            ));
        }

        return list;
    }


    public List<PetsitterReviewResponseDTO> getUserReviews(String uid) {
        QuerySnapshot snapshots = null;
        try {
            snapshots = firestore.collection("users")
                    .document(uid)
                    .collection("psReviews")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().get();
        } catch (Exception e) {
            throw new ReviewException("리뷰 조회에 실패했습니다.");
        }

        List<PetsitterReviewResponseDTO> list = new ArrayList<>();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            list.add(new PetsitterReviewResponseDTO(
                    doc.getId(),
                    doc.getString("userUid"),
                    doc.getLong("rating").intValue(),
                    doc.getString("content"),
                    doc.getTimestamp("createdAt")
            ));
        }

        return list;
    }

    public void updateReview(
            String uid,
            String petsitterId,
            String reviewId,
            ReviewUpdateDTO req
    ) {
        DocumentReference psReviewRef = firestore.collection("petsitters") // 펫시터가 가진 리뷰 레퍼런스
                .document(petsitterId)
                .collection("reviews")
                .document(reviewId);

        DocumentReference userReviewRef = firestore.collection("users")
                .document(uid)
                .collection("psReviews")
                .document(reviewId);
        try {
            firestore.runTransaction(tx -> {

                DocumentSnapshot review = tx.get(psReviewRef).get();

                if (!review.exists()) {
                    throw new ReviewException("리뷰가 존재하지 않습니다.");
                }

                if (!uid.equals(review.getString("userUid"))) {
                    throw new ReviewException("리뷰 수정 권한이 없습니다.");
                }

                int oldRating = review.getLong("rating").intValue();
                int newRating = req.getRating();

                if (oldRating == newRating &&
                        req.getContent().equals(review.getString("content"))) {
                    return null;
                }

                DocumentReference petsitterRef =
                        firestore.collection("petsitters")
                                .document(petsitterId);

                DocumentSnapshot petsitter = tx.get(petsitterRef).get();

                double currentTemp =
                        petsitter.contains("mannerTemp")
                                ? petsitter.getDouble("mannerTemp")
                                : 36.5;

                long reviewCount =
                        petsitter.getLong("reviewCount");

                double currentAvgRating =
                        petsitter.getDouble("rating");

                double newAvgRating =
                        ((currentAvgRating * reviewCount)
                                - oldRating
                                + newRating)
                                / reviewCount;

                double tempDelta =
                        ratingTemp(newRating)
                                - ratingTemp(oldRating);

                double newTemp =
                        Math.max(0, Math.min(100, currentTemp + tempDelta));
                newTemp = Math.round(newTemp * 10) / 10.0;

                tx.update(psReviewRef,
                        "rating", newRating,
                        "content", req.getContent(),
                        "updatedAt", Timestamp.now()
                );

                tx.update(userReviewRef,
                        "rating", newRating,
                        "content", req.getContent(),
                        "updatedAt", Timestamp.now()
                );

                tx.update(petsitterRef,
                        "rating", Math.round(newAvgRating * 10) / 10.0,
                        "mannerTemp", newTemp
                );

                return null;
            }).get();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void deleteReview(String uid, String petsitterId, String reviewId) {

        DocumentReference psReviewRef = firestore.collection("petsitters")
                .document(petsitterId)
                .collection("reviews")
                .document(reviewId);

        DocumentReference userReviewRef = firestore.collection("users")
                .document(uid)
                .collection("psReviews")
                .document(reviewId);

        try {
            firestore.runTransaction(tx -> {
                DocumentSnapshot review = tx.get(psReviewRef).get();

                if (!review.exists()) {
                    throw new ReviewException("리뷰가 존재하지 않습니다.");
                }

                if (!uid.equals(review.getString("userUid"))) {
                    throw new ReviewException("리뷰 삭제 권한이 없습니다.");
                }

                int rating = review.getLong("rating").intValue();

                DocumentReference petsitterRef = firestore.collection("petsitters")
                        .document(petsitterId);

                DocumentSnapshot petsitter = tx.get(petsitterRef).get();

                long reviewCount = petsitter.getLong("reviewCount");

                if (reviewCount <= 1) {
                    tx.update(petsitterRef,
                            "reviewCount", reviewCount - 1,
                            "rating", 0.0,
                            "mannerTemp", 36.5);
                } else {
                    double currAvgRating = petsitter.getDouble("rating");
                    double currTemp = petsitter.getDouble("mannerTemp");

                    double newAvgRating = ((currAvgRating * reviewCount) - rating) / (reviewCount - 1); // 평점 다시 계산
                    double newTemp = currTemp + ratingTemp(rating);

                    newTemp = Math.max(0, Math.min(100, newTemp));
                    newTemp = Math.round(newTemp * 10) / 10.0;

                    tx.update(petsitterRef,
                            "reviewCount", reviewCount - 1,
                            "rating", Math.round(newAvgRating * 10) / 10.0,
                            "mannerTemp", newTemp
                    );
                }

                tx.delete(psReviewRef);
                tx.delete(userReviewRef);

                return null;
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double ratingTemp(int rating) {
        return switch (rating) {
            case 5 -> 0.3;
            case 4 -> 0.1;
            case 3 -> 0.0;
            case 2 -> -0.1;
            case 1 -> -0.3;
            default -> 0.0;
        };
    }

//    private double convertMannerTemp(double currTemp, int rating) {
//
//        double ratio = switch (rating) {
//            case 5 -> 0.3;
//            case 4 -> 0.1;
//            case 3 -> 0.0;
//            case 2 -> -0.1;
//            case 1 -> -0.3;
//            default -> 0.0;
//        };
//
//        double result = currTemp + ratio;
//
//        return result;
//    }
}
