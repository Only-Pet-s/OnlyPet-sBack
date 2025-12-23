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
public class ShortsCommentService {

    private final Firestore firestore;
    private final CommentCoreService core;
    private final FirestoreDeleteUtil deleteUtil;


    //댓글 생성
    public CommentResponse create(String shortsId, String uid, CommentRequest req)
            throws Exception {
        DocumentReference ref = firestore.collection("shorts").document(shortsId);

        //트랜잭션 : 생성 + 카운트 증가
        String commentId = firestore.runTransaction(tx -> {
            DocumentSnapshot s = tx.get(ref).get();
            if (!s.exists())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (!Boolean.TRUE.equals(s.getBoolean("commentAvailable")))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            
            String cid = core.createInternal(tx, "shorts", shortsId, uid, req);

            Long cnt = Optional.ofNullable(s.getLong("commentCount")).orElse(0L);
            tx.update(ref, "commentCount", cnt + 1);

            return cid;
        }).get();

        //트랜잭션 종료 후 조회
        return core.getOne("shorts", shortsId, commentId, uid);
    }

    //가져오기
    public List<CommentResponse> get(String shortsId, String uid) throws Exception {
        return core.getTree("shorts", shortsId, uid);
    }

    //수정
    public CommentResponse update(String shortsId, String commentId, 
            String uid, CommentRequest req) throws Exception {
        core.update("shorts", shortsId, commentId, uid, req);
        return core.getOne("shorts", shortsId, commentId, uid);
    }

    //삭제
    public void delete(String shortsId, String commentId, String uid) throws Exception {

        DocumentReference shortsRef =
                firestore.collection("shorts").document(shortsId);
        DocumentReference commentRef =
                core.validateAndGetRef("shorts", shortsId, commentId, uid);

        deleteUtil.deleteDocumentWithSubcollections(commentRef);

        firestore.runTransaction(tx -> {
            DocumentSnapshot s = tx.get(shortsRef).get();
            Long cnt = Optional.ofNullable(s.getLong("commentCount")).orElse(0L);
            tx.update(shortsRef, "commentCount", Math.max(0, cnt - 1));
            return null;
        }).get();
    }

    // 댓글 좋아요
    public void like(String shortsId, String commentId, String uid) throws Exception {
        core.like("shorts", shortsId, commentId, uid);
    }

    // 댓글 좋아요 취소
    public void unlike(String shortsId, String commentId, String uid) throws Exception {
        core.unlike("shorts", shortsId, commentId, uid);
    }
}
