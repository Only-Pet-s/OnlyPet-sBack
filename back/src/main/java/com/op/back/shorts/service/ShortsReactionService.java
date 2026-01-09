package com.op.back.shorts.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** 좋아요/북마크 등 리액션 */
@Service
@RequiredArgsConstructor
public class ShortsReactionService {

    private final Firestore firestore;

    private static final String SHORTS = "shorts";

    // 좋아요
    public void likeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);
        DocumentReference likeRef = sRef.collection("likes").document(uid);

        DocumentReference userLikeRef = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("shorts")
                .collection("items")
                .document(shortsId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(likeRef).get();
            DocumentSnapshot shortsSnap = tx.get(sRef).get();

            if (!likeSnap.exists()) {
                tx.set(likeRef, Map.of("likedAt", Timestamp.now()));

                Long count = Optional.ofNullable(shortsSnap.getLong("likeCount")).orElse(0L);
                tx.update(sRef, "likeCount", count + 1);

                tx.set(userLikeRef, Map.of(
                        "shortsId", shortsId,
                        "likedAt", Timestamp.now()
                ));
            }
            return null;
        }).get();
    }

    // 좋아요 취소
    public void unlikeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);
        DocumentReference likeRef = sRef.collection("likes").document(uid);

        DocumentReference userLikeRef = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("shorts")
                .collection("items")
                .document(shortsId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(likeRef).get();
            DocumentSnapshot shortsSnap = tx.get(sRef).get();

            if (likeSnap.exists()) {
                Long count = Optional.ofNullable(shortsSnap.getLong("likeCount")).orElse(0L);
                tx.delete(likeRef);
                tx.update(sRef, "likeCount", Math.max(0L, count - 1));

                tx.delete(userLikeRef);
            }
            return null;
        }).get();
    }

    // 북마크 추가
    public void bookmarkShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference bookmarkRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("shorts")
                .collection("items")
                .document(shortsId);

        DocumentReference shortsBookmarkRef = firestore
                .collection("shorts")
                .document(shortsId)
                .collection("bookmarks")
                .document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookmarkRef).get();
            if (!snap.exists()) {
                tx.set(bookmarkRef, Map.of(
                        "shortsId", shortsId,
                        "bookmarkedAt", Timestamp.now()
                ));

                tx.set(shortsBookmarkRef, Map.of(
                        "uid", uid,
                        "bookmarkedAt", Timestamp.now()
                ));
            }
            return null;
        }).get();
    }

    // 북마크 제거
    public void unbookmarkShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference bookmarkRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("shorts")
                .collection("items")
                .document(shortsId);

        DocumentReference shortsBookmarkRef = firestore
                .collection("shorts")
                .document(shortsId)
                .collection("bookmarks")
                .document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookmarkRef).get();
            if (snap.exists()) {
                tx.delete(bookmarkRef);
                tx.delete(shortsBookmarkRef);
            }
            return null;
        }).get();
    }

    public boolean isLiked(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        if (uid == null) return false;
        DocumentSnapshot snap = firestore.collection(SHORTS)
                .document(shortsId)
                .collection("likes")
                .document(uid)
                .get()
                .get();
        return snap.exists();
    }

    public boolean isBookmarked(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        if (uid == null) return false;
        DocumentSnapshot snap = firestore.collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("shorts")
                .collection("items")
                .document(shortsId)
                .get()
                .get();
        return snap.exists();
    }
}
