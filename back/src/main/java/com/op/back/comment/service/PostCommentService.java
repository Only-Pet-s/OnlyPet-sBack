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
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final Firestore firestore;
    private final CommentCoreService core;

    //댓글 생성
    public String create(String postId, String uid, CommentRequest req)
            throws Exception {
        DocumentSnapshot postDoc = firestore.collection("posts").document(postId).get().get();
        if (!postDoc.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        boolean commentAvailable = postDoc.getBoolean("commentAvailable") != null
                ? postDoc.getBoolean("commentAvailable")
                : true;

        // commentCount 증가 → PostService가 아닌 여기서 해도 됨
        incrementCommentCount(postId);

        return core.createComment("posts", postId, uid, req, commentAvailable);
    }

    //가져오기
    public List<CommentResponse> get(String postId, String uid)
            throws ExecutionException, InterruptedException {
        return core.getComments("posts", postId, uid);
    }



    public void update(String postId, String commentId, String uid, CommentRequest req)
            throws Exception {
        core.updateComment("posts", postId, commentId, uid, req);
    }


    public void delete(String postId, String commentId, String uid)
            throws Exception {
        core.deleteComment("posts", postId, commentId, uid);
        decrementCommentCount(postId);
    }


    public void like(String postId, String commentId, String uid) throws Exception {
        core.likeComment("posts", postId, commentId, uid);
    }


    public void unlike(String postId, String commentId, String uid) throws Exception {
        core.unlikeComment("posts", postId, commentId, uid);
    }


    private void incrementCommentCount(String postId) throws Exception {
        DocumentReference postRef = firestore.collection("posts").document(postId);
        firestore.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(postRef).get();
            Long c = doc.getLong("commentCount");
            if (c == null) c = 0L;
            tx.update(postRef, "commentCount", c + 1);
            return null;
        }).get();
    }

    private void decrementCommentCount(String postId) throws Exception {
        DocumentReference postRef = firestore.collection("posts").document(postId);
        firestore.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(postRef).get();
            Long c = doc.getLong("commentCount");
            if (c == null) c = 0L;
            tx.update(postRef, "commentCount", Math.max(0, c - 1));
            return null;
        }).get();
    }
}