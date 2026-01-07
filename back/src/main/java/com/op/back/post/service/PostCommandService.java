package com.op.back.post.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.dto.PostUpdateRequest;
import com.op.back.post.search.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/*
* 생성/수정/삭제(Command) 
*/
@Service
@RequiredArgsConstructor
public class PostCommandService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;
    private final PostCleanupService cleanupService;
    private final PostMediaService postMediaService;
    private final PostSearchService postSearchService;
    private final PostQueryService postQueryService;

    private static final String POSTS_COLLECTION = "posts";

    // 게시글 생성
    public PostResponse createPost(PostCreateRequest request, List<MultipartFile> mediaFiles, String uid)
            throws IOException, ExecutionException, InterruptedException {

        if (mediaFiles != null && mediaFiles.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "media max 5");
        }
        String postId = UUID.randomUUID().toString();

        // 1. 포스트 문서 생성
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("content", request.getContent());
        data.put("hashtags", Optional.ofNullable(request.getHashtags()).orElse(List.of()));
        data.put("commentAvailable",
                request.getCommentAvailable() != null ? request.getCommentAvailable() : true
        );
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("viewCount", 0L);
        data.put("createdAt", Timestamp.now());

        // 호환 필드 (첫 번째 미디어)
        data.put("mediaUrl", null);
        data.put("mediaType", null);
        data.put("mediaCount", 0L);

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        postRef.set(data).get();

        // 2) media subcollection 저장
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            int order = 0;
            for (MultipartFile file : mediaFiles) {
                if (file == null || file.isEmpty()) continue;
                postMediaService.createMediaDocument(postRef, uid, postId, file, order++);
            }
        }

        // 3) 첫 번째 미디어를 루트 문서에 반영 (피드 호환용)
        postMediaService.syncFirstMediaToPostRoot(postRef);

        // 4) ES 동기화 (검색용 - 미디어 타입은 첫 번째 기준)
        postSearchService.upsert(
                PostDocument.builder()
                        .id(postId)
                        .uid(uid)
                        .content(request.getContent())
                        .hashtags(Optional.ofNullable(request.getHashtags()).orElse(List.of()))
                        .mediaType(postMediaService.getFirstMediaType(postRef))
                        .likeCount(0)
                        .commentCount(0)
                        .createdAt(Instant.now().toString())
                        .build()
        );

        // 생성 직후 상세조회해서 Response 반환
        return postQueryService.getPost(postId, uid);
    }

    // 게시글 수정 (미디어: 추가/삭제/순서변경)
    public PostResponse updatePost(String postId, PostUpdateRequest request,
                                  List<MultipartFile> newMediaFiles, String currentUid) throws Exception {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentSnapshot snap = postRef.get().get();
        if (!snap.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(snap.getString("uid"), currentUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // 1) 텍스트/옵션 업데이트
        Map<String, Object> postUpdates = new HashMap<>();
        if (request.getContent() != null) postUpdates.put("content", request.getContent());
        if (request.getHashtags() != null) postUpdates.put("hashtags", request.getHashtags());
        if (request.getCommentAvailable() != null) postUpdates.put("commentAvailable", request.getCommentAvailable());
        if (!postUpdates.isEmpty()) {
            postRef.update(postUpdates).get();
        }

        // 2) 삭제 요청 처리
        if (request.getDeleteMediaIds() != null && !request.getDeleteMediaIds().isEmpty()) {
            for (String mediaId : request.getDeleteMediaIds()) {
                if (mediaId == null || mediaId.isBlank()) continue;
                DocumentReference mediaRef = postRef.collection("media").document(mediaId);
                DocumentSnapshot mediaSnap = mediaRef.get().get();
                if (!mediaSnap.exists()) continue;

                // Storage 삭제
                String mediaUrl = mediaSnap.getString("mediaUrl");
                String thumbUrl = mediaSnap.getString("thumbnailUrl");
                storageService.deleteFileByUrl(mediaUrl);
                // IMAGE는 thumbnailUrl == mediaUrl일 수 있음
                if (thumbUrl != null && !thumbUrl.isBlank() && !Objects.equals(thumbUrl, mediaUrl)) {
                    storageService.deleteFileByUrl(thumbUrl);
                }

                mediaRef.delete().get();
            }
        }

        // 3) 추가 업로드 처리 (기본: 마지막에 append)
        if (newMediaFiles != null && !newMediaFiles.isEmpty()) {
            int currentCount = postMediaService.getMediaCount(postRef);
            int incoming = (int) newMediaFiles.stream().filter(f -> f != null && !f.isEmpty()).count();
            if (currentCount + incoming > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "media max 5");
            }

            int startOrder = postMediaService.getMaxOrder(postRef) + 1;
            int order = startOrder;
            for (MultipartFile file : newMediaFiles) {
                if (file == null || file.isEmpty()) continue;
                postMediaService.createMediaDocument(postRef, currentUid, postId, file, order++);
            }
        }

        // 4) 재정렬 처리 (order 값만 업데이트)
        if (request.getReorder() != null && !request.getReorder().isEmpty()) {
            for (com.op.back.post.dto.PostMediaReorderRequest r : request.getReorder()) {
                if (r == null || r.getMediaId() == null || r.getMediaId().isBlank()) continue;
                if (r.getOrder() == null) continue;
                postRef.collection("media").document(r.getMediaId())
                        .update(Map.of("order", r.getOrder()))
                        .get();
            }
        }

        // 5) order 정규화 (0..n-1)
        postMediaService.normalizeOrders(postRef);

        // 6) 루트 문서(첫 미디어/카운트) 동기화
        postMediaService.syncFirstMediaToPostRoot(postRef);

        // 7) ES 동기화
        Timestamp ts = snap.getTimestamp("createdAt");
        Instant createdAt = ts != null ? Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()) : null;
        String createdAtStr = createdAt != null ? createdAt.toString() : Instant.now().toString();
        postSearchService.upsert(
                PostDocument.builder()
                        .id(postId)
                        .uid(currentUid)
                        .content(request.getContent() != null ? request.getContent() : snap.getString("content"))
                        .hashtags(request.getHashtags() != null ? request.getHashtags() : (List<String>) snap.get("hashtags"))
                        .mediaType(postMediaService.getFirstMediaType(postRef))
                        .likeCount(Optional.ofNullable(snap.getLong("likeCount")).orElse(0L).intValue())
                        .commentCount(Optional.ofNullable(snap.getLong("commentCount")).orElse(0L).intValue())
                        .createdAt(createdAtStr)
                        .build()
        );

        return postQueryService.getPost(postId, currentUid);
    }

    // 게시글 삭제(only owner)
    public void deletePost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);
        DocumentSnapshot doc = postRef.get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        String ownerUid = doc.getString("uid");
        if (!Objects.equals(ownerUid, currentUid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        // 삭제는 모두 CleanupService가 처리 (media 서브컬렉션의 모든 파일까지 삭제)
        cleanupService.cleanupPost(postRef, postId);

        // ES 삭제
        try {
            postSearchService.delete(postId);
        } catch (Exception e) {
            // log only
        }
    }
}
