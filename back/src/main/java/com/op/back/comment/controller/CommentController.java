package com.op.back.comment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.PostCommentService;
import com.op.back.comment.service.ShortsCommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final PostCommentService postService;
    private final ShortsCommentService shortsService;

    private String currentUid() {
        return (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // 댓글 생성
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                switch (req.getTargetType()) {
                    case "POST" -> postService.create(
                            req.getTargetId(), currentUid(), req
                    );
                    case "SHORTS" -> shortsService.create(
                            req.getTargetId(), currentUid(), req
                    );
                    default -> throw new IllegalArgumentException("invalid targetType");
                }
        );
    }

    // 댓글 목록 (트리)
    @GetMapping
    public ResponseEntity<List<CommentResponse>> get(
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        return ResponseEntity.ok(
                switch (targetType) {
                    case "POST" -> postService.get(targetId, currentUid());
                    case "SHORTS" -> shortsService.get(targetId, currentUid());
                    default -> throw new IllegalArgumentException("invalid targetType");
                }
        );
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable String commentId,
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                switch (req.getTargetType()) {
                    case "POST" -> postService.update(
                            req.getTargetId(), commentId, currentUid(), req
                    );
                    case "SHORTS" -> shortsService.update(
                            req.getTargetId(), commentId, currentUid(), req
                    );
                    default -> throw new IllegalArgumentException("invalid targetType");
                }
        );
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        switch (targetType) {
            case "POST" -> postService.delete(targetId, commentId, currentUid());
            case "SHORTS" -> shortsService.delete(targetId, commentId, currentUid());
            default -> throw new IllegalArgumentException("invalid targetType");
        }

        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        switch (targetType) {
            case "POST" -> postService.like(targetId, commentId, currentUid());
            case "SHORTS" -> shortsService.like(targetId, commentId, currentUid());
            default -> throw new IllegalArgumentException("invalid targetType");
        }

        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        switch (targetType) {
            case "POST" -> postService.unlike(targetId, commentId, currentUid());
            case "SHORTS" -> shortsService.unlike(targetId, commentId, currentUid());
            default -> throw new IllegalArgumentException("invalid targetType");
        }

        return ResponseEntity.ok().build();
    }
}