package com.op.back.post.service;


import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.common.service.FirestoreDeleteUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PostCleanupService {
    private final Firestore firestore;
    private final FirestoreDeleteUtil firestoreDeleteUtil;
    private final PostMediaService postMediaService;


    private static final String USERS_COLLECTION = "users";

    public void cleanupPost(DocumentReference postRef, String postId)
            throws ExecutionException, InterruptedException {

        // 1. media 정리 위임
        postMediaService.cleanupAllMedia(postRef);

        // 2. Firestore 문서 + 하위 컬렉션 삭제
        firestoreDeleteUtil.deleteDocumentWithSubcollections(postRef);

        // 3. user bookmarks / likes 정리
        deleteAllBookmarks(postId);
        deleteAllUserLikes(postId);
    }

    private void deleteAllBookmarks(String postId)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> users = firestore
                .collection(USERS_COLLECTION)
                .get()
                .get()
                .getDocuments();

        for (DocumentSnapshot user : users) {
            DocumentReference bookmarkRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(user.getId())
                    .collection("bookmarks")
                    .document("posts")
                    .collection("items")
                    .document(postId);

            if (bookmarkRef.get().get().exists()) {
                bookmarkRef.delete().get();
            }
        }
    }

    private void deleteAllUserLikes(String postId)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> users = firestore
                .collection("users")
                .get()
                .get()
                .getDocuments();

        for (DocumentSnapshot user : users) {
            DocumentReference likeRef = firestore
                    .collection("users")
                    .document(user.getId())
                    .collection("likes")
                    .document("posts")
                    .collection("items")
                    .document(postId);

            if (likeRef.get().get().exists()) {
                likeRef.delete().get();
            }
        }
    }
}
