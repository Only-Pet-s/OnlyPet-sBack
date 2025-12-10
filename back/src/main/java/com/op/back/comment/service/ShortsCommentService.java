package com.op.back.comment.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShortsCommentService {

    private final Firestore firestore;
    private final CommentCoreService core;

    public String create(String shortsId, String uid, CommentRequest req)
            throws Exception {

        DocumentSnapshot doc = firestore.collection("shorts").document(shortsId).get().get();

        if (!doc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        boolean allow = doc.getBoolean("commentAvailable") != null
                ? doc.getBoolean("commentAvailable")
                : true;

        incrementCommentCount(shortsId);

        return core.createComment("shorts", shortsId, uid, req, allow);
    }

    public List<CommentResponse> get(String shortsId, String uid) throws Exception {
        return core.getComments("shorts", shortsId, uid);
    }

    public void update(String shortsId, String commentId, String uid, CommentRequest req)
            throws Exception {
        core.updateComment("shorts", shortsId, commentId, uid, req);
    }

    public void delete(String shortsId, String commentId, String uid)
            throws Exception {
        core.deleteComment("shorts", shortsId, commentId, uid);
        decrementCommentCount(shortsId);
    }

    public void like(String shortsId, String commentId, String uid) throws Exception {
        core.likeComment("shorts", shortsId, commentId, uid);
    }

    public void unlike(String shortsId, String commentId, String uid) throws Exception {
        core.unlikeComment("shorts", shortsId, commentId, uid);
    }

    private void incrementCommentCount(String shortsId) throws Exception {
        DocumentReference ref = firestore.collection("shorts").document(shortsId);
        firestore.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(ref).get();
            Long c = doc.getLong("commentCount");
            if (c == null) c = 0L;
            tx.update(ref, "commentCount", c + 1);
            return null;
        }).get();
    }

    private void decrementCommentCount(String shortsId) throws Exception {
        DocumentReference ref = firestore.collection("shorts").document(shortsId);
        firestore.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(ref).get();
            Long c = doc.getLong("commentCount");
            if (c == null) c = 0L;
            tx.update(ref, "commentCount", Math.max(0, c - 1));
            return null;
        }).get();
    }
}
