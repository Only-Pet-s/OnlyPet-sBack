package com.op.back.comment.controller;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.PostCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService service;

    // JWT 인증된 사용자 UID
    private String currentUid() {
        return (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // 댓글 생성
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable String postId,
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
            service.create(postId, currentUid(), req)
        );
    }

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponse>> get(
            @PathVariable String postId
    ) throws Exception {

        return ResponseEntity.ok(
                service.get(postId, currentUid())
        );
    }

    // 댓글 수정 (작성자만 가능)
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable String postId,
            @PathVariable String commentId,
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                service.update(postId, commentId, currentUid(), req)
        );
    }

    // 댓글 삭제 (작성자만 가능)
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String postId,
            @PathVariable String commentId
    ) throws Exception {

        service.delete(postId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(
            @PathVariable String postId,
            @PathVariable String commentId
    ) throws Exception {

        service.like(postId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable String postId,
            @PathVariable String commentId
    ) throws Exception {

        service.unlike(postId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }
}
