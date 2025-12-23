package com.op.back.comment.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentFacadeService {

    private final PostCommentService postService;
    private final ShortsCommentService shortsService;

    //댓글 생성
    public CommentResponse create(String uid, CommentRequest req) throws Exception {
        return switch (req.getTargetType()) {
            case "POST" ->
                postService.create(req.getTargetId(), uid, req);
            case "SHORTS" ->
                shortsService.create(req.getTargetId(), uid, req);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        };
    }

    //댓글 목록 조회
    public List<CommentResponse> get(String targetType,String targetId,String uid) throws Exception {
        return switch (targetType) {
            case "POST" ->
                postService.get(targetId, uid);
            case "SHORTS" ->
                shortsService.get(targetId, uid);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        };
    }

    //댓글 수정
    public CommentResponse update(String commentId,String uid,CommentRequest req) throws Exception {
        return switch (req.getTargetType()) {
            case "POST" ->
                postService.update(req.getTargetId(), commentId, uid, req);
            case "SHORTS" ->
                shortsService.update(req.getTargetId(), commentId, uid, req);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        };
    }

    //댓글 삭제
    public void delete(String commentId,String targetType,String targetId,String uid) throws Exception {
        switch (targetType) {
            case "POST" ->
                postService.delete(targetId, commentId, uid);
            case "SHORTS" ->
                shortsService.delete(targetId, commentId, uid);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        }
    }

    //댓글 좋아요
    public void like(String commentId,String targetType,String targetId,String uid) throws Exception {
        switch (targetType) {
            case "POST" ->
                postService.like(targetId, commentId, uid);
            case "SHORTS" ->
                shortsService.like(targetId, commentId, uid);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        }
    }

    //댓글 좋아요 취소
    public void unlike(String commentId,String targetType,String targetId,String uid) throws Exception {
        switch (targetType) {
            case "POST" ->
                postService.unlike(targetId, commentId, uid);
            case "SHORTS" ->
                shortsService.unlike(targetId, commentId, uid);
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid targetType");
        }
    }
}