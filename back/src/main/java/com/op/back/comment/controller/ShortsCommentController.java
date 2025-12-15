package com.op.back.comment.controller;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.ShortsCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shorts/{shortsId}/comments")
@RequiredArgsConstructor
public class ShortsCommentController {

    private final ShortsCommentService service;

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
            @PathVariable String shortsId,
            @RequestBody CommentRequest req
    ) throws Exception {

        return ResponseEntity.ok(
                service.create(shortsId, currentUid(), req)
        );
    }

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<CommentResponse>> get(
            @PathVariable String shortsId
    ) throws Exception {

        return ResponseEntity.ok(
                service.get(shortsId, currentUid())
        );
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable String shortsId,
            @PathVariable String commentId,
            @RequestBody CommentRequest req
    ) throws Exception {

        service.update(shortsId, commentId, currentUid(), req);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String shortsId,
            @PathVariable String commentId
    ) throws Exception {

        service.delete(shortsId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> like(
            @PathVariable String shortsId,
            @PathVariable String commentId
    ) throws Exception {

        service.like(shortsId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlike(
            @PathVariable String shortsId,
            @PathVariable String commentId
    ) throws Exception {

        service.unlike(shortsId, commentId, currentUid());
        return ResponseEntity.ok().build();
    }
}
