package com.op.back.comment.service;

import com.google.cloud.firestore.*;
import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.common.service.FirestoreDeleteUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final Firestore firestore;
    private final CommentCoreService core;
    private final FirestoreDeleteUtil deleteUtil;


    //댓글 생성
    public CommentResponse create(String postId, String uid, CommentRequest req)
            throws Exception {
        DocumentReference postRef = firestore.collection("posts").document(postId);

        //생성 담당 트랜잭션
        String commentId = firestore.runTransaction(tx -> {
            DocumentSnapshot post = tx.get(postRef).get();

            if (!post.exists())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (!Boolean.TRUE.equals(post.getBoolean("commentAvailable")))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);

            String cid = core.createInternal(tx, "posts", postId, uid, req);

            Long cnt = Optional.ofNullable(post.getLong("commentCount")).orElse(0L);
            tx.update(postRef, "commentCount", cnt + 1);

            return cid;
        }).get();

        //트랜잭션 종료 후 조회
        return core.getOne("posts", postId, commentId, uid);
    }

    //가져오기
    public List<CommentResponse> get(String postId, String uid) throws Exception {
        return core.getTree("posts", postId, uid);
    }

    //업데이트
    public CommentResponse update(String postId, String commentId, 
            String uid, CommentRequest req) throws Exception {
        core.update("posts", postId, commentId, uid, req);
        return core.getOne("posts", postId, commentId, uid);
    }


    public void delete(String postId, String commentId, String uid) throws Exception {
        DocumentReference postRef =
                firestore.collection("posts").document(postId);
        //권한체크 및 ref반환
        DocumentReference commentRef =
                core.validateAndGetRef("posts", postId, commentId, uid);
        //댓글 + 하위 컬렉션 삭제
        deleteUtil.deleteDocumentWithSubcollections(commentRef);

        //commentCount 감소
        firestore.runTransaction(tx -> {
            DocumentSnapshot post = tx.get(postRef).get();
            Long cnt = Optional.ofNullable(post.getLong("commentCount")).orElse(0L);
            tx.update(postRef, "commentCount", Math.max(0, cnt - 1));
            return null;
        }).get();
    }

    // 댓글 좋아요 
    public void like(String postId, String commentId, String uid) throws Exception {
        core.like("posts", postId, commentId, uid);
    }

    // 댓글 좋아요 취소
    public void unlike(String postId, String commentId, String uid) throws Exception {
        core.unlike("posts", postId, commentId, uid);
    }
}