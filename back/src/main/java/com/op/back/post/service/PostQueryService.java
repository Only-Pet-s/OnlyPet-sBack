package com.op.back.post.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** 조회/리스트/해시태그(Firestore) */
@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final Firestore firestore;
    private final PostMapperService postMapperService;
    private final PostReactionService postReactionService;
    private final PostViewService postViewService;

    private static final String POSTS_COLLECTION = "posts";

    // 최신 게시글 목록 조회
    public List<PostResponse> getLatestPosts(int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        List<PostResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Post post = postMapperService.toPost(doc);
            boolean liked = postReactionService.isLikedByUser(post.getId(), currentUid);
            boolean bookmarked = postReactionService.isBookmarkedByUser(post.getId(), currentUid);
            result.add(postMapperService.toListResponse(post, liked, bookmarked, currentUid));
        }

        return result;
    }

    // 단일 게시글 조회
    public PostResponse getPost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {

        DocumentSnapshot doc = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .get()
                .get();

        if (!doc.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }

        // 조회수 처리
        postViewService.handleViewCount(postId, currentUid);

        Post post = postMapperService.toPost(doc);
        boolean liked = postReactionService.isLikedByUser(post.getId(), currentUid);
        boolean bookmarked = postReactionService.isBookmarkedByUser(post.getId(), currentUid);

        return postMapperService.toDetailResponse(post, liked, bookmarked, currentUid);
    }

    // 해시태그 검색
    public List<PostResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection(POSTS_COLLECTION)
                .whereArrayContains("hashtags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

        List<PostResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Post post = postMapperService.toPost(doc);
            boolean liked = postReactionService.isLikedByUser(post.getId(), currentUid);
            boolean bookmarked = postReactionService.isBookmarkedByUser(post.getId(), currentUid);
            result.add(postMapperService.toListResponse(post, liked, bookmarked, currentUid));
        }

        return result;
    }
}
