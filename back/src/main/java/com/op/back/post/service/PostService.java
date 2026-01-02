package com.op.back.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.dto.PostUpdateRequest;
import com.op.back.post.model.Post;
import com.op.back.post.search.PostDocument;
import com.op.back.post.search.PostSearchRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {
    private final Firestore firestore;
    private final FirebaseStorageService storageService;
    private final PostCleanupService cleanupService;
    //RedisTemplate
    private final StringRedisTemplate redisTemplate;
    private static final String POSTS_COLLECTION = "posts";
    private final PostSearchRepository postSearchRepository;

    //게시글 생성
    public PostResponse createPost(PostCreateRequest request,MultipartFile mediaFile,
        MultipartFile thumbnail, String uid)
            throws IOException, ExecutionException, InterruptedException {

        String postId = UUID.randomUUID().toString();
        String mediaUrl = storageService.uploadFile(
                mediaFile,
                "posts/" + uid + "/" + postId
        );

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("content", request.getContent());
        data.put("mediaUrl", mediaUrl);
        data.put("mediaType", request.getMediaType());
        data.put("hashtags", Optional.ofNullable(request.getHashtags()).orElse(List.of()));
        data.put("commentAvailable",
                request.getCommentAvailable() != null ? request.getCommentAvailable() : true
        );
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("viewCount", 0L);
        data.put("createdAt", Timestamp.now());

        firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .set(data)
                .get();

        postSearchRepository.save(
            PostDocument.builder()
                    .id(postId)
                    .uid(uid)
                    .content(request.getContent())
                    .hashtags(
                            Optional.ofNullable(request.getHashtags())
                                    .orElse(List.of())
                    )
                    .mediaType(request.getMediaType())
                    .likeCount(0)
                    .commentCount(0)
                    .createdAt(Instant.now())
                    .build()
    );

        // 생성 직후 상세조회해서 Response 반환
        return getPost(postId, uid);
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
            result.add(
                toListResponse(post, liked, bookmarked, currentUid)
            );
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

        //조회수 처리
        handleViewCount(postId, currentUid);

        Post post = toPost(doc);
        boolean liked = isLikedByUser(post.getId(),currentUid);
        boolean bookmarked = isBookmarkedByUser(post.getId(),currentUid);

        return toDetailResponse(
                post,
                liked,
                bookmarked,
                currentUid
        );
    }

    //게시글 수정
    public PostResponse updatePost(String postId,PostUpdateRequest request,MultipartFile mediaFile,
            MultipartFile thumbnailFile,String currentUid) throws Exception {

        DocumentReference postRef = firestore.collection("posts").document(postId);
        DocumentSnapshot snap = postRef.get().get();
        if (!snap.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!Objects.equals(snap.getString("uid"), currentUid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        Map<String, Object> updates = new HashMap<>();

        if (mediaFile != null && !mediaFile.isEmpty()) {
            String oldMediaUrl = snap.getString("mediaUrl");
            if (oldMediaUrl != null) storageService.deleteFile(oldMediaUrl);

            String newMediaUrl = storageService.uploadFile(mediaFile, "posts/" + currentUid + "/" + postId);
            updates.put("mediaUrl", newMediaUrl);

            // mediaType이 요청에 오면 반영 (안 오면 기존 유지)
            String mediaType = request.getMediaType() != null ? request.getMediaType() : snap.getString("mediaType");
            if (mediaType != null) updates.put("mediaType", mediaType);

            // 썸네일이 따로 안 오면 자동 생성
            if (thumbnailFile == null || thumbnailFile.isEmpty()) {
                if ("IMAGE".equalsIgnoreCase(mediaType)) {
                    updates.put("thumbnailUrl", newMediaUrl);
                } else {
                    byte[] thumbBytes = com.op.back.common.util.VideoThumbnailUtil.extractJpegBytes(mediaFile);
                    String autoThumbUrl = storageService.uploadBytes(thumbBytes, "image/jpeg", "posts/" + currentUid + "/" + postId + "/thumbnail.jpg");
                    updates.put("thumbnailUrl", autoThumbUrl);
                }
            }
        }

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            String oldThumbUrl = snap.getString("thumbnailUrl");
            if (oldThumbUrl != null) storageService.deleteFile(oldThumbUrl);
            String newThumbUrl = storageService.uploadFile(thumbnailFile, "posts/" + currentUid + "/" + postId + "/thumbnail.jpg");
            updates.put("thumbnailUrl", newThumbUrl);
        }

        if (request.getContent() != null)
            updates.put("content", request.getContent());
        if (request.getHashtags() != null)
            updates.put("hashtags", request.getHashtags());
        if (!updates.isEmpty())
            postRef.update(updates).get();

        //createdAt설정
        Instant createdAt = null;
        Timestamp ts = snap.getTimestamp("createdAt");
        if (ts != null) {
            createdAt = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
        }

        // ES 동기화
        postSearchRepository.save(
            PostDocument.builder()
                .id(postId)
                .uid(currentUid)
                .content(request.getContent() != null? request.getContent(): snap.getString("content"))
                .hashtags(request.getHashtags() != null? request.getHashtags(): (List<String>) snap.get("hashtags"))
                .mediaType(snap.getString("mediaType"))
                .likeCount(Optional.ofNullable(snap.getLong("likeCount")).orElse(0L).intValue())
                .commentCount(Optional.ofNullable(snap.getLong("commentCount")).orElse(0L).intValue())
                .createdAt(createdAt)
                .build()
        );

        return getPost(postId, currentUid);
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
        
        //ES 삭제
        try {
            postSearchRepository.delete(postId);
        } catch (Exception e) {
            // log.warn("ES delete failed", e);
        }
    }

    //좋아요 추가
    public void likePost(String postId, String uid)
        throws ExecutionException, InterruptedException {

        DocumentReference postRef =
                firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef =
                postRef.collection("likes").document(uid);
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

                Long likeCount =
                        Optional.ofNullable(postSnap.getLong("likeCount")).orElse(0L);

                // 그 다음 WRITE
                tx.set(likeRef, Map.of("likedAt", Timestamp.now()));
                tx.update(postRef, "likeCount", likeCount + 1);

                //user 기준 저장(이중)
                tx.set(userLikeRef, Map.of(
                        "postId", postId,
                        "likedAt", Timestamp.now()
                ));
            }

            return null;
        }).get();
    }

    //좋아요 취소
    public void unlikePost(String postId, String uid) throws ExecutionException, InterruptedException {
        DocumentReference postRef =
            firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentReference likeRef =
                postRef.collection("likes").document(uid);
        
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
                Long likeCount =
                        Optional.ofNullable(postSnap.getLong("likeCount")).orElse(0L);
                tx.delete(likeRef);
                tx.update(postRef, "likeCount", Math.max(0L, likeCount - 1));

                //user기준 삭제
                tx.delete(userLikeRef);
            }

            return null;
        }).get();
    }

    //북마크 추가
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


    //해쉬태그 검색
    public List<PostResponse> searchByHashtag(String tag,int limit,
            String currentUid) throws ExecutionException, InterruptedException {

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

            result.add(
                    toListResponse(post, liked, bookmarked, currentUid)
            );
        }

        return result;
    }

    /*
        엘라스틱 서치 기반 검색 
    */
    public List<PostResponse> search(String q) {
        return postSearchRepository.search(q).stream()
                .map(this::toSearchResponse)
                .toList();
    }

    //조회수 증가
    private void increaseViewCount(String postId)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef =
                firestore.collection(POSTS_COLLECTION).document(postId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(postRef).get();
            Long count = Optional.ofNullable(snap.getLong("viewCount")).orElse(0L);
            tx.update(postRef, "viewCount", count + 1);
            return null;
        }).get();
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
                .viewCount(Optional.ofNullable(doc.getLong("viewCount")).orElse(0L))
                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    private PostResponse toListResponse(Post post,boolean liked,
            boolean bookmarked,String currentUid){
        return PostResponse.builder()
                .id(post.getId())
                .uid(post.getUid())
                .nickname(getNickname(post.getUid()))
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .commentAvailable(post.getCommentAvailable())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .mine(post.getUid().equals(currentUid))
                .createdAt(toInstant(post.getCreatedAt()))
                .build();
    }

    private PostResponse toDetailResponse(Post post,boolean liked,boolean bookmarked,
            String currentUid) {
        return PostResponse.builder()
                .id(post.getId())
                .uid(post.getUid())
                .nickname(getNickname(post.getUid()))
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .commentAvailable(post.getCommentAvailable())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .mine(post.getUid().equals(currentUid))
                .createdAt(toInstant(post.getCreatedAt()))
                .build();
    }

    /**
     * Elasticsearch 검색 결과 → PostResponse 변환
     * (Firestore 조회 안 함, 검색 전용)
     */
    private PostResponse toSearchResponse(com.op.back.post.search.PostDocument doc) {
        return PostResponse.builder()
                .id(doc.getId())
                .uid(doc.getUid())
                .nickname(getNickname(doc.getUid())) // 기존 로직 재사용
                .content(doc.getContent())
                .mediaType(doc.getMediaType())
                .hashtags(doc.getHashtags())
                .likeCount(doc.getLikeCount())
                .commentCount(doc.getCommentCount())
                .viewCount(0L)
                .liked(false)
                .bookmarked(false)
                .mine(false)
                .createdAt(doc.getCreatedAt())
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

    private Instant toInstant(Timestamp ts) {
        if (ts == null) return null;
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    //닉네임 조회
    private String getNickname(String uid) {
        if(uid == null) return null;
        try{
            DocumentSnapshot userDoc = firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .get();

            if (!userDoc.exists()) return null;
            return userDoc.getString("nickname");
        } catch (Exception e) {
            return null;
        }
    }

    //레디스 기반 조회수 처리
    private void handleViewCount(String postId, String uid)
            throws ExecutionException, InterruptedException {
        
        // 비로그인 유저 제외        
        if (uid == null) return; 
        String key = "post:view:" + postId + ":" + uid;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        // Redis에 기록 (TTL 10분)
        redisTemplate.opsForValue()
                .set(key, "1", 10, TimeUnit.MINUTES);

        // Firestore 증가
        increaseViewCount(postId);
    }


    //내가 누른 좋아요 가져오기.
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
                    .collection("posts")
                    .document(postId)
                    .get()
                    .get();

            if (!postDoc.exists()) continue;

            Post post = toPost(postDoc);

            result.add(
                    toListResponse(
                            post,
                            true,   // liked
                            false,  // bookmarked (원하면 체크 가능)
                            uid
                    )
            );
        }
        return result;
    }

    //내가 누른 북마크 조회
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
                    .collection("posts")
                    .document(postId)
                    .get()
                    .get();

            if (!postDoc.exists()) continue;

            Post post = toPost(postDoc);

            result.add(
                    toListResponse(
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
