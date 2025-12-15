package com.op.back.comment.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentCoreService {

    private final Firestore firestore;

    // 내부 ref
    CollectionReference comments(String parentCol, String parentId) {
        return firestore.collection(parentCol).document(parentId).collection("comments");
    }

    DocumentReference commentRef(String parentCol, String parentId, String commentId) {
        return comments(parentCol, parentId).document(commentId);
    }

    //  댓글 생성
    String createInternal(
            Transaction tx,
            String parentCol,
            String parentId,
            String uid,
            CommentRequest req
    ) {
        String commentId = UUID.randomUUID().toString();
        DocumentReference ref = commentRef(parentCol, parentId, commentId);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("content", req.getContent());
        data.put("parentId", req.getParentId()); // 대댓글용
        data.put("likeCount", 0L);
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", null);
        data.put("edited", false);

        tx.set(ref, data);
        return commentId;
    }

    // 댓글 수정
    public void update(
            String parentCol,
            String parentId,
            String commentId,
            String uid,
            CommentRequest req
    ) throws Exception {

        DocumentReference ref = commentRef(parentCol, parentId, commentId);
        DocumentSnapshot doc = ref.get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        if (!uid.equals(doc.getString("uid")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        ref.update(
                Map.of(
                        "content", req.getContent(),
                        "updatedAt", Timestamp.now(),
                        "edited", true
                )
        ).get();
    }

    // 권한체크 & ref반환
    public DocumentReference validateAndGetRef(String parentCol,String parentId,
            String commentId,String uid) throws Exception {

        DocumentReference ref = commentRef(parentCol, parentId, commentId);
        DocumentSnapshot doc = ref.get().get();
        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!uid.equals(doc.getString("uid")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        return ref;
    }

    // 댓글 좋아요
    public void like(String parentCol, String parentId, String commentId, String uid)
            throws Exception {

        DocumentReference cRef = commentRef(parentCol, parentId, commentId);
        DocumentReference lRef = cRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(lRef).get();
            if (!likeSnap.exists()) {
                tx.set(lRef, Map.of("likedAt", Timestamp.now()));

                DocumentSnapshot cSnap = tx.get(cRef).get();
                Long cnt = Optional.ofNullable(cSnap.getLong("likeCount")).orElse(0L);
                tx.update(cRef, "likeCount", cnt + 1);
            }
            return null;
        }).get();
    }

    // 댓글 좋아요 취소
    public void unlike(String parentCol, String parentId, String commentId, String uid)
            throws Exception {

        DocumentReference cRef = commentRef(parentCol, parentId, commentId);
        DocumentReference lRef = cRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(lRef).get();
            if (likeSnap.exists()) {
                tx.delete(lRef);

                DocumentSnapshot cSnap = tx.get(cRef).get();
                Long cnt = Optional.ofNullable(cSnap.getLong("likeCount")).orElse(0L);
                tx.update(cRef, "likeCount", Math.max(0, cnt - 1));
            }
            return null;
        }).get();
    }

    // 댓글 조회
    public List<CommentResponse> getTree(String parentCol,String parentId,
        String uid) throws Exception {

        // 1. flat 조회
        List<QueryDocumentSnapshot> docs = comments(parentCol, parentId)
                .orderBy("createdAt")
                .get()
                .get()
                .getDocuments();

        // 2. id → CommentResponse 맵
        Map<String, CommentResponse> map = new LinkedHashMap<>();

        for (DocumentSnapshot d : docs) {
            boolean liked = isLiked(parentCol, parentId, d.getId(), uid);
            CommentResponse cr = toResponse(d, liked)
                    .toBuilder()
                    .children(new ArrayList<>())
                    .build();
            map.put(cr.getId(), cr);
        }

        // 3. 트리 조립
        List<CommentResponse> roots = new ArrayList<>();

        for (CommentResponse cr : map.values()) {
            if (cr.getParentId() == null) {
                roots.add(cr);
            } else {
                CommentResponse parent = map.get(cr.getParentId());
                if (parent != null) {
                    parent.getChildren().add(cr);
                }
            }
        }

        return roots;
    }


    boolean isLiked(String parentCol, String parentId, String commentId, String uid)
            throws Exception {

        if (uid == null) return false;

        return commentRef(parentCol, parentId, commentId)
                .collection("likes")
                .document(uid)
                .get()
                .get()
                .exists();
    }

    public CommentResponse getOne(String parentCol,String parentId,
        String commentId,String uid) throws Exception {

        DocumentSnapshot doc = commentRef(parentCol, parentId, commentId).get().get();
        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        boolean liked = isLiked(parentCol, parentId, commentId, uid);
        return toResponse(doc, liked);
    }


    // 댓글 응답 변환
    private CommentResponse toResponse(DocumentSnapshot doc, boolean liked) {
        Timestamp created = doc.getTimestamp("createdAt");
        Timestamp updated = doc.getTimestamp("updatedAt");

        Instant createdAt = created != null
                ? Instant.ofEpochSecond(created.getSeconds(), created.getNanos())
                : null;

        Instant updatedAt = updated != null
                ? Instant.ofEpochSecond(updated.getSeconds(), updated.getNanos())
                : null;

        return CommentResponse.builder()
                .id(doc.getId())
                .parentId(doc.getString("parentId"))

                .uid(doc.getString("uid"))
                .nickname(getNickname(doc.getString("uid")))

                .content(doc.getString("content"))
                .likeCount(
                    Optional.ofNullable(doc.getLong("likeCount")).orElse(0L)
                )
                .liked(liked)
                
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .edited(Boolean.TRUE.equals(doc.getBoolean("edited")))
                .build();
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
}
