package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.op.back.petsitter.dto.PetsitterReviewResponseDTO;
import com.op.back.petsitter.exception.ReviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PetsitterReviewQueryService {

    private final Firestore firestore;

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
                    doc.getString("nickname"),
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
        DocumentSnapshot userSnap = null;
        try {
            userSnap =
                    firestore.collection("users")
                            .document(uid)
                            .get()
                            .get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        String nickname = userSnap.getString("nickname");
        List<PetsitterReviewResponseDTO> list = new ArrayList<>();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            list.add(new PetsitterReviewResponseDTO(
                    doc.getId(),
                    doc.getString("userUid"),
                    nickname,
                    doc.getLong("rating").intValue(),
                    doc.getString("content"),
                    doc.getTimestamp("createdAt")
            ));
        }

        return list;
    }
}
