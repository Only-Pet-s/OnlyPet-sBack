package com.op.back.shorts.service;

import com.google.cloud.firestore.*;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.model.Shorts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ShortsMyQueryService {

    private final Firestore firestore;
    private final ShortsMapperService shortsMapperService;

    private static final String SHORTS = "shorts";

    // 내가 좋아요 누른 쇼츠
    public List<ShortsResponse> getLikedShorts(String uid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> likeDocs = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("shorts")
                .collection("items")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments();

        List<ShortsResponse> result = new ArrayList<>();

        for (DocumentSnapshot likeDoc : likeDocs) {
            String shortsId = likeDoc.getId();

            DocumentSnapshot shortsDoc = firestore
                    .collection(SHORTS)
                    .document(shortsId)
                    .get()
                    .get();

            if (!shortsDoc.exists()) continue;

            Shorts shorts = shortsMapperService.toShorts(shortsDoc);

            result.add(
                    shortsMapperService.toResponse(
                            shorts,
                            true,   // liked
                            false,  // bookmarked
                            uid
                    )
            );
        }
        return result;
    }

    // 내가 북마크한 쇼츠
    public List<ShortsResponse> getBookmarkedShorts(String uid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> bookmarkDocs = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("shorts")
                .collection("items")
                .orderBy("bookmarkedAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments();

        List<ShortsResponse> result = new ArrayList<>();

        for (DocumentSnapshot bmDoc : bookmarkDocs) {
            String shortsId = bmDoc.getId();

            DocumentSnapshot shortsDoc = firestore
                    .collection(SHORTS)
                    .document(shortsId)
                    .get()
                    .get();

            if (!shortsDoc.exists()) continue;

            Shorts shorts = shortsMapperService.toShorts(shortsDoc);

            result.add(
                    shortsMapperService.toResponse(
                            shorts,
                            false, // liked
                            true,  // bookmarked
                            uid
                    )
            );
        }
        return result;
    }
}
