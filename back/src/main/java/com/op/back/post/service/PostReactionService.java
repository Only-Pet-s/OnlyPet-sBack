package com.op.back.post.service;

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
public class PostReactionService {

    private final Firestore firestore;
    private static final String POSTS_COLLECTION = "posts";

    // 좋아요 추가
    public void likePost(String postId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);
        DocumentReference userLikeRef = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("posts")
                .collection("items")
                .document(postId);

        firestore.runTransaction(tx -> {

            // 모든 READ 먼저
            DocumentSnapshot likeSnap = tx.get(likeRef).get();
            DocumentSnapshot postSnap = tx.get(postRef).get();

            if (!likeSnap.exists()) {

                Long likeCount = Optional.ofNullable(postSnap.getLong("likeCount")).orElse(0L);

                // 그 다음 WRITE
                tx.set(likeRef, Map.of("likedAt", Timestamp.now()));
                tx.update(postRef, "likeCount", likeCount + 1);

                // user 기준 저장(이중)
                tx.set(userLikeRef, Map.of(
                        "postId", postId,
                        "likedAt", Timestamp.now()
                ));
            }

            return null;
        }).get();
    }

    // 좋아요 취소
    public void unlikePost(String postId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);

        DocumentReference userLikeRef = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("posts")
                .collection("items")
                .document(postId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(likeRef).get();
            DocumentSnapshot postSnap = tx.get(postRef).get();

            if (likeSnap.exists()) {
                Long likeCount = Optional.ofNullable(postSnap.getLong("likeCount")).orElse(0L);
                tx.delete(likeRef);
                tx.update(postRef, "likeCount", Math.max(0L, likeCount - 1));

                // user 기준 삭제
                tx.delete(userLikeRef);
            }

            return null;
        }).get();
    }

    // 북마크 추가
    public void bookmarkPost(String postId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference bookmarkRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .document(postId);

        DocumentReference postBookmarkRef = firestore
                .collection("posts")
                .document(postId)
                .collection("bookmarks")
                .document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookmarkRef).get();
            if (!snap.exists()) {
                tx.set(bookmarkRef, Map.of(
                        "postId", postId,
                        "bookmarkedAt", Timestamp.now()
                ));
                tx.set(postBookmarkRef, Map.of(
                        "uid", uid,
                        "bookmarkedAt", Timestamp.now()
                ));
            }
            return null;
        }).get();
    }

    // 북마크 제거
    public void unbookmarkPost(String postId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference bookmarkRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .document(postId);

        DocumentReference postBookmarkRef = firestore
                .collection("posts")
                .document(postId)
                .collection("bookmarks")
                .document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(bookmarkRef).get();
            if (snap.exists()) {
                tx.delete(bookmarkRef);
                tx.delete(postBookmarkRef);
            }
            return null;
        }).get();
    }

    public boolean isLikedByUser(String postId, String uid)
            throws ExecutionException, InterruptedException {
        if (uid == null) return false;
        DocumentSnapshot snap = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection("likes")
                .document(uid)
                .get()
                .get();
        return snap.exists();
    }

    public boolean isBookmarkedByUser(String postId, String uid)
            throws ExecutionException, InterruptedException {
        if (uid == null) return false;
        DocumentSnapshot snap = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .document(postId)
                .get()
                .get();
        return snap.exists();
    }
}
