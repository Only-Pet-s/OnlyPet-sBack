package com.op.back.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;

import com.op.back.common.service.FirebaseStorageService;
import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.model.Post;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PostService {
    private final Firestore firestore;
    private final FirebaseStorageService storageService;

    private final PostCleanupService cleanupService;

    private static final String POSTS_COLLECTION = "posts";

    //게시글 생성
    public String createPost(PostCreateRequest request,MultipartFile mediaFile, String uid)
            throws IOException, ExecutionException, InterruptedException {
        String postId = UUID.randomUUID().toString();

        String mediaUrl = storageService.uploadFile(
                mediaFile,
                "posts/"+uid+"/"+postId
        );

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("content", request.getContent());
        data.put("mediaUrl", mediaUrl);
        data.put("mediaType", request.getMediaType());
        data.put("hashtags", request.getHashtags() != null ? request.getHashtags() : Collections.emptyList());
        data.put("commentAvailable",
                request.getCommentAvailable() != null ? request.getCommentAvailable() : true
        );
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("createdAt", Timestamp.now());

        firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .set(data)
                .get();

        return postId;
    }


    //최신 게시글 목록 조회
    public List<PostResponse> getLatestPosts(int limit, String currentUid)
        throws ExecutionException, InterruptedException {

        Query query = firestore.collection(POSTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        List<PostResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs ){
            Post post = toPost(doc);
            boolean liked = isLikedByUser(post.getId(),currentUid);
            boolean bookmarked = isBookmarkedByUser(post.getId(),currentUid);
            result.add(toResponse(post,liked, bookmarked));
        }

        return result;
    }


    //단일 게시글 조회
    public PostResponse getPost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .get()
                .get();

        if(!doc.exists()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Post not found");
        }

        Post post = toPost(doc);
        boolean liked = isLikedByUser(post.getId(),currentUid);
        boolean bookmarked = isBookmarkedByUser(post.getId(),currentUid);

        return toResponse(post,liked,bookmarked);
    }

    //게시글 삭제(only owner)
    public void deletePost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentSnapshot doc = postRef.get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        String ownerUid = doc.getString("uid");
        if (!Objects.equals(ownerUid, currentUid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        String mediaUrl = doc.getString("mediaUrl");

        // 삭제는 모두 CleanupService가 처리
        cleanupService.cleanupPost(postRef, postId, mediaUrl);
    }

    //좋아요 추가
    public void likePost(String postId,String uid) throws ExecutionException, InterruptedException {
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot likeSnap = transaction.get(likeRef).get();
            if(!likeSnap.exists()){
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("likedAt", Timestamp.now());
                transaction.set(likeRef, likeData);

                DocumentSnapshot postSnap = transaction.get(postRef).get();
                Long likeCount = postSnap.getLong("likeCount");
                if (likeCount == null) likeCount = 0L;
                transaction.update(postRef,"likeCount",likeCount+1);
            }
            return null;
        }).get();
    }

    //좋아요 취소
    public void unlikePost(String postId, String uid) throws ExecutionException, InterruptedException {
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(uid);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot likeSnap = transaction.get(likeRef).get();
            if (likeSnap.exists()) {
                transaction.delete(likeRef);

                DocumentSnapshot postSnap = transaction.get(postRef).get();
                Long likeCount = postSnap.getLong("likeCount");
                if (likeCount == null) likeCount = 0L;
                long newCount = Math.max(0L, likeCount - 1);
                transaction.update(postRef, "likeCount", newCount);
            }
            return null;
        }).get();
    }

    //북마크 추가
    public void bookmarkPost(String postId,String uid)
            throws ExecutionException, InterruptedException {
        DocumentReference bookmarRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .document(postId);

        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("bookmarkedAt", Timestamp.now());

        bookmarRef.set(data).get();
    }


    //북마크 제거
    public void unbookmarkPost(String postId, String uid)
        throws ExecutionException, InterruptedException {

        DocumentReference bookmarkRef = firestore
                .collection("users")
                .document(uid)
                .collection("bookmarks")
                .document("posts")
                .collection("items")
                .document(postId);

        bookmarkRef.delete().get();
    }


    //해쉬태그 검색
    public List<PostResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection(POSTS_COLLECTION)
                .whereArrayContains("hashtags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

        List<PostResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Post post = toPost(doc);
            boolean liked = isLikedByUser(post.getId(), currentUid);
            boolean bookmarked = isBookmarkedByUser(post.getId(), currentUid);
            result.add(toResponse(post, liked, bookmarked));
        }
        return result;
    }



    // **내부 유틸 메서드** //
    private Post toPost(DocumentSnapshot doc) {
        return Post.builder()
                .id(doc.getId())
                .uid(doc.getString("uid"))
                .content(doc.getString("content"))
                .mediaUrl(doc.getString("mediaUrl"))
                .mediaType(doc.getString("mediaType"))
                .hashtags((List<String>) doc.get("hashtags"))
                .commentAvailable(doc.getBoolean("commentAvailable"))
                .likeCount(Optional.ofNullable(doc.getLong("likeCount")).orElse(0L))
                .commentCount(Optional.ofNullable(doc.getLong("commentCount")).orElse(0L))
                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    private PostResponse toResponse(Post post, boolean liked, boolean bookmarked) {
        Instant created = post.getCreatedAt() != null
                ? Instant.ofEpochSecond(post.getCreatedAt().getSeconds(), post.getCreatedAt().getNanos())
                : null;

        return PostResponse.builder()
                .id(post.getId())
                .uid(post.getUid())
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .commentAvailable(post.getCommentAvailable())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .createdAt(created)
                .build();
    }

    private boolean isLikedByUser(String postId, String uid)
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

    private boolean isBookmarkedByUser(String postId, String uid)
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
