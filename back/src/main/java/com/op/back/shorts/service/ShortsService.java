package com.op.back.shorts.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.model.Shorts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ShortsService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;

    private static final String SHORTS = "shorts";

    // 쇼츠 생성
    public String createShorts(ShortsCreateRequest request,
                               MultipartFile videoFile,
                               String uid)
            throws IOException, ExecutionException, InterruptedException {

        if (videoFile == null || videoFile.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "video required");

        String shortsId = UUID.randomUUID().toString();

        // Storage upload
        String videoUrl = storageService.uploadFile(
                videoFile,
                "shorts/" + uid + "/" + shortsId
        );

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("mediaUrl", videoUrl);
        data.put("thumbnailUrl", null); // TODO: 썸네일 생성시 업로드
        data.put("description", request.getDescription());
        data.put("hashtags",
                request.getHashtags() != null ? request.getHashtags() : Collections.emptyList());
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("viewCount", 0L);
        data.put("createdAt", Timestamp.now());

        firestore.collection(SHORTS)
                .document(shortsId)
                .set(data)
                .get();

        return shortsId;
    }

    // 쇼츠 단일 조회
    public ShortsResponse getShorts(String shortsId, String currentUid)
            throws ExecutionException, InterruptedException {

        DocumentSnapshot doc = firestore.collection(SHORTS)
                .document(shortsId)
                .get()
                .get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        Shorts shorts = toShorts(doc);
        boolean liked = isLiked(shortsId, currentUid);
        boolean bookmarked = isBookmarked(shortsId, currentUid);

        return toResponse(shorts, liked, bookmarked);
    }

    // 최신 쇼츠 피드 조회
    public List<ShortsResponse> getLatestShorts(int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> docs = firestore.collection(SHORTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get()
                .getDocuments();

        List<ShortsResponse> result = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            Shorts s = toShorts(doc);
            boolean liked = isLiked(s.getId(), currentUid);
            boolean bookmarked = isBookmarked(s.getId(), currentUid);
            result.add(toResponse(s, liked, bookmarked));
        }

        return result;
    }

    // 좋아요
    public void likeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);
        DocumentReference likeRef = sRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(likeRef).get();

            if (!snap.exists()) {
                tx.set(likeRef, Map.of("likedAt", Timestamp.now()));

                DocumentSnapshot sDoc = tx.get(sRef).get();
                Long count = sDoc.getLong("likeCount");
                if (count == null) count = 0L;

                tx.update(sRef, "likeCount", count + 1);
            }
            return null;
        }).get();
    }

    // 좋아요 취소
    public void unlikeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);
        DocumentReference likeRef = sRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(likeRef).get();
            if (snap.exists()) {
                tx.delete(likeRef);

                DocumentSnapshot sDoc = tx.get(sRef).get();
                Long count = sDoc.getLong("likeCount");
                if (count == null) count = 0L;

                tx.update(sRef, "likeCount", Math.max(0, count - 1));
            }
            return null;
        }).get();
    }

    // 조회수 증가
    public void increaseViewCount(String shortsId)
            throws ExecutionException, InterruptedException {

        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(sRef).get();

            Long vc = doc.getLong("viewCount");
            if (vc == null) vc = 0L;

            tx.update(sRef, "viewCount", vc + 1);

            return null;
        }).get();
    }


    // 해시태그 검색
    public List<ShortsResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection(SHORTS)
                .whereArrayContains("hashtags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

        List<ShortsResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Shorts s = toShorts(doc);
            boolean liked = isLiked(s.getId(), currentUid);
            boolean bookmarked = isBookmarked(s.getId(), currentUid);
            result.add(toResponse(s, liked, bookmarked));
        }
        return result;
    }

    // **내부 유틸** //
    private Shorts toShorts(DocumentSnapshot doc) {
        return Shorts.builder()
                .id(doc.getId())
                .uid(doc.getString("uid"))
                .mediaUrl(doc.getString("mediaUrl"))
                .thumbnailUrl(doc.getString("thumbnailUrl"))
                .description(doc.getString("description"))
                .hashtags((List<String>) doc.get("hashtags"))
                .likeCount(doc.getLong("likeCount"))
                .commentCount(doc.getLong("commentCount"))
                .viewCount(doc.getLong("viewCount"))
                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    private ShortsResponse toResponse(Shorts s, boolean liked, boolean bookmarked) {
        Instant created = s.getCreatedAt() != null
                ? Instant.ofEpochSecond(s.getCreatedAt().getSeconds(), s.getCreatedAt().getNanos())
                : null;

        return ShortsResponse.builder()
                .id(s.getId())
                .uid(s.getUid())
                .mediaUrl(s.getMediaUrl())
                .thumbnailUrl(s.getThumbnailUrl())
                .description(s.getDescription())
                .hashtags(s.getHashtags())
                .likeCount(s.getLikeCount())
                .commentCount(s.getCommentCount())
                .viewCount(s.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .createdAt(created)
                .build();
    }

    private boolean isLiked(String shortsId, String uid)
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

    private boolean isBookmarked(String shortsId, String uid)
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
