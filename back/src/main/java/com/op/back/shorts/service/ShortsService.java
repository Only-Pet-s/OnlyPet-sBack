package com.op.back.shorts.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.model.Shorts;
import com.op.back.shorts.search.ShortsSearchRepository;
import com.op.back.shorts.search.ShortsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ShortsService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;
    private final StringRedisTemplate redisTemplate;

    private static final String SHORTS = "shorts";
    private final ShortsSearchRepository shortsSearchRepository;

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

        shortsSearchRepository.save(
                ShortsDocument.builder()
                        .id(shortsId)
                        .uid(uid)
                        .description(request.getDescription())
                        .hashtags(
                                request.getHashtags() != null
                                        ? request.getHashtags()
                                        : List.of()
                        )
                        .viewCount(0)
                        .createdAt(Instant.now())
                        .build()
        );
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

        handleViewCount(shortsId, currentUid);

        Shorts s = toShorts(doc);
        boolean liked = isLiked(shortsId, currentUid);
        boolean bookmarked = isBookmarked(shortsId, currentUid);

        return toResponse(s, liked, bookmarked, currentUid);
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

            result.add(
                    toResponse(s, liked, bookmarked, currentUid)
            );
        }
        return result;
    }

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

    // 조회수 증가
    private void increaseViewCount(String shortsId)
        throws ExecutionException, InterruptedException {
        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(sRef).get();
            Long count = Optional.ofNullable(snap.getLong("viewCount")).orElse(0L);
            tx.update(sRef, "viewCount", count + 1);
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
            result.add(toResponse(s, liked, bookmarked, currentUid));
        }
        return result;
    }

    //북마크 추가
    public void bookmarkShorts(String shortsId, String uid) throws ExecutionException, InterruptedException {
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

    //북마크 제거
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

    /*
        엘라스틱 서치 기반 검색
    */
    public List<ShortsResponse> search(String q) {
        return shortsSearchRepository.search(q).stream()
                .map(this::toSearchResponse)
                .toList();
    }


    //내가 누른 좋아요 조회
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
                    .collection("shorts")
                    .document(shortsId)
                    .get()
                    .get();

            if (!shortsDoc.exists()) continue;

            Shorts s = toShorts(shortsDoc);

            result.add(
                    toResponse(
                            s,
                            true,   // liked
                            false,  // bookmarked
                            uid
                    )
            );
        }
        return result;
    }

    //내가 누른 북마크 조회
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
                    .collection("shorts")
                    .document(shortsId)
                    .get()
                    .get();

            if (!shortsDoc.exists()) continue;

            Shorts s = toShorts(shortsDoc);

            result.add(
                    toResponse(
                            s,
                            false, // liked
                            true,  // bookmarked
                            uid
                    )
            );
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

                .likeCount(Optional.ofNullable(doc.getLong("likeCount")).orElse(0L))
                .commentCount(Optional.ofNullable(doc.getLong("commentCount")).orElse(0L))
                .viewCount(Optional.ofNullable(doc.getLong("viewCount")).orElse(0L))

                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    private ShortsResponse toResponse( Shorts s,boolean liked,boolean bookmarked,
        String currentUid) {
        Instant createdAt = s.getCreatedAt() != null
                ? Instant.ofEpochSecond(
                        s.getCreatedAt().getSeconds(),
                        s.getCreatedAt().getNanos()
                )
                : null;

        return ShortsResponse.builder()
                .id(s.getId())

                .uid(s.getUid())
                .nickname(getNickname(s.getUid()))

                .mediaUrl(s.getMediaUrl())
                .thumbnailUrl(s.getThumbnailUrl())
                .description(s.getDescription())
                .hashtags(s.getHashtags())

                .likeCount(s.getLikeCount())
                .commentCount(s.getCommentCount())
                .viewCount(s.getViewCount())

                .liked(liked)
                .bookmarked(bookmarked)
                .mine(s.getUid().equals(currentUid))

                .createdAt(createdAt)
                .build();
    }

    /**
     * Elasticsearch 검색 결과 → ShortsResponse 변환
     * (Firestore 재조회 X, 검색 전용)
     */
    private ShortsResponse toSearchResponse(ShortsDocument doc) {
        return ShortsResponse.builder()
                .id(doc.getId())
                .uid(doc.getUid())
                .nickname(getNickname(doc.getUid()))

                // 검색 리스트에서는 영상 URL/썸네일만 있으면 충분
                .mediaUrl(null)
                .thumbnailUrl(null)

                .description(doc.getDescription())
                .hashtags(doc.getHashtags())

                .likeCount(0L)
                .commentCount(0L)
                .viewCount(doc.getViewCount())

                .liked(false)
                .bookmarked(false)
                .mine(false)

                .createdAt(doc.getCreatedAt())
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

    // 닉네임 조회
    private String getNickname(String uid) {
        if (uid == null) return null;
        try {
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

    // 조회수 처리
    private void handleViewCount(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        // 비로그인 유저는 제외 (원하면 허용 가능)
        if (uid == null) return;

        String key = "shorts:view:" + shortsId + ":" + uid;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return; // 이미 조회 처리됨
        }

        // Redis 기록 (TTL 10분)
        redisTemplate.opsForValue()
                .set(key, "1", 10, TimeUnit.MINUTES);

        // Firestore 조회수 증가
        increaseViewCount(shortsId);
    }
}
