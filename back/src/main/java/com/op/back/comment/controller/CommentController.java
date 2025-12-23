package com.op.back.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.CommentFacadeService;
import com.op.back.comment.service.PostCommentService;
import com.op.back.comment.service.ShortsCommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentFacadeService commentFacadeService;

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

        String uid = currentUid();

        return ResponseEntity.ok(
            commentFacadeService.create(uid, req)
        );
    }

    // 댓글 목록 (트리)
    @GetMapping
    public ResponseEntity<List<CommentResponse>> get(
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        return ResponseEntity.ok(
                commentFacadeService.get(targetType, targetId, currentUid())
        );
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable String commentId,
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                commentFacadeService.update(commentId, currentUid(), req)
        );
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        commentFacadeService.delete(commentId, targetType, targetId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        commentFacadeService.like(commentId, targetType, targetId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable String commentId,
            @RequestParam String targetType,
            @RequestParam String targetId
    ) throws Exception {

        commentFacadeService.unlike(commentId, targetType, targetId, currentUid());
        return ResponseEntity.ok().build();
    }
}