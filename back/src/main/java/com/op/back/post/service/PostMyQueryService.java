package com.op.back.post.service;

import com.google.cloud.firestore.*;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PostMyQueryService {

    private final Firestore firestore;
    private final PostMapperService postMapperService;

    private static final String POSTS = "posts";

    // 내가 좋아요 누른 포스트
    public List<PostResponse> getLikedPosts(String uid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> likeDocs = firestore
                .collection("users")
                .document(uid)
                .collection("likes")
                .document("posts")
                .collection("items")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments();

        List<PostResponse> result = new ArrayList<>();

        for (DocumentSnapshot likeDoc : likeDocs) {
            String postId = likeDoc.getId();

            DocumentSnapshot postDoc = firestore
                    .collection(POSTS)
                    .document(postId)
                    .get()
                    .get();

            if (!postDoc.exists()) continue;

            Post post = postMapperService.toPost(postDoc);

            result.add(
                    postMapperService.toListResponse(
                            post,
                            true,   // liked
                            false,  // bookmarked
                            uid
                    )
            );
        }
        return result;
    }

    // 내가 북마크한 포스트
    public List<PostResponse> getBookmarkedPosts(String uid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> bookmarkDocs = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .orderBy("bookmarkedAt", Query.Direction.DESCENDING)
                .get()
                .get()
                .getDocuments();

        List<PostResponse> result = new ArrayList<>();

        for (DocumentSnapshot bmDoc : bookmarkDocs) {
            String postId = bmDoc.getId();

            DocumentSnapshot postDoc = firestore
                    .collection(POSTS)
                    .document(postId)
                    .get()
                    .get();

            if (!postDoc.exists()) continue;

            Post post = postMapperService.toPost(postDoc);

            result.add(
                    postMapperService.toListResponse(
                            post,
                            false, // liked
                            true,  // bookmarked
                            uid
                    )
            );
        }
        return result;
    }
}
