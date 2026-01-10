package com.op.back.shorts.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.common.service.FirebaseStorageService;
import com.op.back.common.service.FirestoreDeleteUtil;
import com.op.back.common.util.VideoThumbnailUtil;
import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.dto.ShortsUpdateRequest;
import com.op.back.shorts.search.ShortsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** 생성/수정/삭제(Command) */
@Service
@RequiredArgsConstructor
public class ShortsCommandService {

    private final Firestore firestore;
    private final FirebaseStorageService storageService;
    private final FirestoreDeleteUtil firestoreDeleteUtil;
    private final ShortsSearchService shortsSearchService;
    private final ShortsQueryService shortsQueryService;

    private static final String SHORTS = "shorts";

    // 쇼츠 생성
    public String createShorts(ShortsCreateRequest request, MultipartFile videoFile,
                               MultipartFile thumbnailFile, String uid)
            throws IOException, ExecutionException, InterruptedException {

        if (videoFile == null || videoFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "video required");
        }

        String shortsId = UUID.randomUUID().toString();

        // Storage upload
        String videoUrl = storageService.uploadFile(
                videoFile,
                "shorts/" + uid + "/" + shortsId
        );

        // 썸네일 처리: 요청O -> 업로드 / 요청X -> 1프레임 추출
        String thumbnailUrl = ensureThumbnail(uid, shortsId, videoFile, thumbnailFile);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("mediaUrl", videoUrl);
        data.put("thumbnailUrl", thumbnailUrl);
        data.put("description", request.getDescription());
        data.put("hashtags", request.getHashtags() != null ? request.getHashtags() : Collections.emptyList());
        data.put("commentAvailable",
                request.getCommentAvailable() != null ? request.getCommentAvailable() : Boolean.TRUE);
        data.put("likeCount", 0L);
        data.put("commentCount", 0L);
        data.put("viewCount", 0L);
        data.put("createdAt", Timestamp.now());

        firestore.collection(SHORTS)
                .document(shortsId)
                .set(data)
                .get();

        shortsSearchService.upsert(
                ShortsDocument.builder()
                        .id(shortsId)
                        .uid(uid)
                        .description(request.getDescription())
                        .hashtags(request.getHashtags() != null ? request.getHashtags() : List.of())
                        .viewCount(0)
                        .createdAt(Instant.now().toString())
                        .build()
        );

        return shortsId;
    }

    // 쇼츠 수정
    public ShortsResponse updateShorts(String shortsId, ShortsUpdateRequest request,
                                      MultipartFile videoFile, MultipartFile thumbnailFile,
                                      String currentUid) throws Exception {

        DocumentReference shortsRef = firestore.collection(SHORTS).document(shortsId);
        DocumentSnapshot snap = shortsRef.get().get();
        if (!snap.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(snap.getString("uid"), currentUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Map<String, Object> updates = new HashMap<>();

        if (request.getDescription() != null) {
            updates.put("description", request.getDescription());
        }
        if (request.getHashtags() != null) {
            updates.put("hashtags", request.getHashtags());
        }

        if (request.getCommentAvailable() != null) {
            updates.put("commentAvailable", request.getCommentAvailable());
        }

        if (request.getCommentAvailable() != null) {
            updates.put("commentAvailable", request.getCommentAvailable());
        }

        // 영상 교체 (옵션)
        if (videoFile != null && !videoFile.isEmpty()) {
            String oldMediaUrl = snap.getString("mediaUrl");
            if (oldMediaUrl != null) {
                storageService.deleteFileByUrl(oldMediaUrl);
            }

            String newVideoUrl = storageService.uploadFile(
                    videoFile,
                    "shorts/" + currentUid + "/" + shortsId
            );
            updates.put("mediaUrl", newVideoUrl);

            // thumbnail이 따로 안 오면 새 영상 기준으로 1프레임 재생성
            if (thumbnailFile == null || thumbnailFile.isEmpty()) {
                String autoThumbUrl = ensureThumbnail(currentUid, shortsId, videoFile, null);
                updates.put("thumbnailUrl", autoThumbUrl);
            }
        }

        // 썸네일 교체 (옵션)
        String thumbnailUrl = snap.getString("thumbnailUrl");
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            if (thumbnailUrl != null) {
                storageService.deleteFileByUrl(thumbnailUrl);
            }
            String newThumbUrl = storageService.uploadFile(
                    thumbnailFile,
                    "shorts/" + currentUid + "/" + shortsId + "/thumbnail"
            );
            updates.put("thumbnailUrl", newThumbUrl);
            thumbnailUrl = newThumbUrl;
        }

        if (!updates.isEmpty()) {
            shortsRef.update(updates).get();
        }

        // Timestamp → Instant (ES 전용)
        Timestamp ts = snap.getTimestamp("createdAt");
        Instant createdAt = ts != null
                ? Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())
                : Instant.now();

        // ES 동기화
        shortsSearchService.upsert(
                ShortsDocument.builder()
                        .id(shortsId)
                        .uid(snap.getString("uid"))
                        .description(request.getDescription() != null
                                ? request.getDescription()
                                : snap.getString("description")
                        )
                        .hashtags(request.getHashtags() != null
                                ? request.getHashtags()
                                : (List<String>) snap.get("hashtags")
                        )
                        .viewCount(Optional.ofNullable(snap.getLong("viewCount")).orElse(0L).intValue())
                        .createdAt(createdAt.toString())
                        .build()
        );

        return shortsQueryService.getShorts(shortsId, currentUid);
    }
    
    // 쇼츠 삭제
    public void deleteShorts(String shortsId, String currentUid) throws Exception {

        DocumentReference shortsRef = firestore.collection(SHORTS).document(shortsId);
        DocumentSnapshot snap = shortsRef.get().get();
        if (!snap.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(snap.getString("uid"), currentUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Storage 삭제
        storageService.deleteFileByUrl(snap.getString("mediaUrl"));
        storageService.deleteFileByUrl(snap.getString("thumbnailUrl"));

        // Firestore 하위 컬렉션 + 본문 삭제
        firestoreDeleteUtil.deleteDocumentWithSubcollections(shortsRef);

        // ES 삭제 (실패해도 전체 흐름 막지 않음)
        try {
            shortsSearchService.delete(shortsId);
        } catch (Exception e) {
            // log only
        }
    }
    
    private String ensureThumbnail(String uid, String shortsId, MultipartFile videoFile, MultipartFile thumbnailFile)
            throws IOException {

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            return storageService.uploadFile(
                    thumbnailFile,
                    "shorts/" + uid + "/" + shortsId + "/thumbnail"
            );
        }

        try {
            byte[] thumbBytes = VideoThumbnailUtil.extractJpegBytes(videoFile);
            return storageService.uploadBytes(
                    thumbBytes,
                    "image/jpeg",
                    "shorts/" + uid + "/" + shortsId + "/thumbnail.jpg"
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "thumbnail generation failed",
                    e
            );
        }
    }
}
