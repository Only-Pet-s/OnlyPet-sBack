package com.op.back.comment.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.common.service.FirestoreDeleteUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CommentCoreService {

    private final Firestore firestore;
    private final FirestoreDeleteUtil deleteUtil;

    // Firestore 기준 /{parentCollection}/{parentId}/comments/{commentId}
    private CollectionReference getCommentCollection(String parentCollection, String parentId) {
        return firestore
                .collection(parentCollection)
                .document(parentId)
                .collection("comments");
    }

    private DocumentReference getCommentRef(String parentCollection, String parentId, String commentId) {
        return getCommentCollection(parentCollection, parentId).document(commentId);
    }

    //  댓글 생성
    public String createComment(
            String parentCollection, String parentId,
            String uid, CommentRequest request,
            boolean parentAllowsComment
    ) throws ExecutionException, InterruptedException {

        if (!parentAllowsComment)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Comments disabled");

        String commentId = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("content", request.getContent());
        data.put("parentId", request.getParentId());
        data.put("likeCount", 0L);
        data.put("createdAt", Timestamp.now());

        getCommentCollection(parentCollection, parentId)
                .document(commentId)
                .set(data)
                .get();

        return commentId;
    }

    // 댓글 수정
    public void updateComment(
            String parentCollection, String parentId,
            String commentId, String uid, CommentRequest request
    ) throws ExecutionException, InterruptedException {

        DocumentReference ref = getCommentRef(parentCollection, parentId, commentId);
        DocumentSnapshot doc = ref.get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        if (!Objects.equals(doc.getString("uid"), uid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        ref.update("content", request.getContent()).get();
    }

    // 댓글 삭제
    public void deleteComment(
            String parentCollection, String parentId,
            String commentId, String uid
    ) throws ExecutionException, InterruptedException {

        DocumentReference ref = getCommentRef(parentCollection, parentId, commentId);
        DocumentSnapshot doc = ref.get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        if (!Objects.equals(doc.getString("uid"), uid))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        deleteUtil.deleteDocumentWithSubcollections(ref);
    }

    // 댓글 좋아요
    public void likeComment(String parentCollection, String parentId, String commentId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference commentRef = getCommentRef(parentCollection, parentId, commentId);
        DocumentReference likeRef = commentRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(likeRef).get();

            if (!likeSnap.exists()) {
                tx.set(likeRef, Map.of("likedAt", Timestamp.now()));

                DocumentSnapshot cSnap = tx.get(commentRef).get();
                Long count = cSnap.getLong("likeCount");
                if (count == null) count = 0L;

                tx.update(commentRef, "likeCount", count + 1);
            }

            return null;
        }).get();
    }

    // 댓글 좋아요 취소
    public void unlikeComment(String parentCollection, String parentId, String commentId, String uid)
            throws ExecutionException, InterruptedException {

        DocumentReference commentRef = getCommentRef(parentCollection, parentId, commentId);
        DocumentReference likeRef = commentRef.collection("likes").document(uid);

        firestore.runTransaction(tx -> {
            DocumentSnapshot likeSnap = tx.get(likeRef).get();

            if (likeSnap.exists()) {
                tx.delete(likeRef);

                DocumentSnapshot cSnap = tx.get(commentRef).get();
                Long count = cSnap.getLong("likeCount");
                if (count == null) count = 0L;

                tx.update(commentRef, "likeCount", Math.max(0L, count - 1));
            }
            return null;
        }).get();
    }

    // 댓글 조회
    public List<CommentResponse> getComments(
            String parentCollection, String parentId, String currentUid
    ) throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> docs = getCommentCollection(parentCollection, parentId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .get()
                .getDocuments();

        List<CommentResponse> result = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            boolean liked = isLiked(parentCollection, parentId, doc.getId(), currentUid);
            result.add(toResponse(doc, liked));
        }

        return result;
    }

    private boolean isLiked(
            String parentCollection, String parentId, String commentId, String uid
    ) throws ExecutionException, InterruptedException {

        if (uid == null) return false;

        DocumentSnapshot snap =
                getCommentRef(parentCollection, parentId, commentId)
                        .collection("likes")
                        .document(uid)
                        .get()
                        .get();

        return snap.exists();
    }

    private CommentResponse toResponse(DocumentSnapshot doc, boolean liked) {
        Timestamp ts = doc.getTimestamp("createdAt");

        return CommentResponse.builder()
                .id(doc.getId())
                .uid(doc.getString("uid"))
                .content(doc.getString("content"))
                .parentId(doc.getString("parentId"))
                .likeCount(doc.getLong("likeCount"))
                .liked(liked)
                .createdAt(ts != null ? Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()) : null)
                .build();
    }
}
