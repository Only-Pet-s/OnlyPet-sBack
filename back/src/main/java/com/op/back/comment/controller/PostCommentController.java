package com.op.back.comment.controller;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.PostCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService service;

    private String uid() { return "TEST_UID"; }

    @PostMapping
    public String create(@PathVariable String postId, @RequestBody CommentRequest req) throws Exception {
        return service.create(postId, uid(), req);
    }

    @GetMapping
    public List<CommentResponse> get(@PathVariable String postId) throws Exception {
        return service.get(postId, uid());
    }

    @PutMapping("/{commentId}")
    public void update(@PathVariable String postId, @PathVariable String commentId,
                       @RequestBody CommentRequest req) throws Exception {
        service.update(postId, commentId, uid(), req);
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable String postId, @PathVariable String commentId) throws Exception {
        service.delete(postId, commentId, uid());
    }

    @PostMapping("/{commentId}/like")
    public void like(@PathVariable String postId, @PathVariable String commentId) throws Exception {
        service.like(postId, commentId, uid());
    }

    @DeleteMapping("/{commentId}/like")
    public void unlike(@PathVariable String postId, @PathVariable String commentId) throws Exception {
        service.unlike(postId, commentId, uid());
    }
}