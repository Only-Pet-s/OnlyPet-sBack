package com.op.back.comment.controller;

import com.op.back.comment.dto.CommentRequest;
import com.op.back.comment.dto.CommentResponse;
import com.op.back.comment.service.ShortsCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shorts/{shortsId}/comments")
@RequiredArgsConstructor
public class ShortsCommentController {

    private final ShortsCommentService service;

    private String uid() { return "TEST_UID"; }

    @PostMapping
    public String create(@PathVariable String shortsId, @RequestBody CommentRequest req) throws Exception {
        return service.create(shortsId, uid(), req);
    }

    @GetMapping
    public List<CommentResponse> get(@PathVariable String shortsId) throws Exception {
        return service.get(shortsId, uid());
    }

    @PutMapping("/{commentId}")
    public void update(@PathVariable String shortsId, @PathVariable String commentId,
                       @RequestBody CommentRequest req) throws Exception {
        service.update(shortsId, commentId, uid(), req);
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable String shortsId, @PathVariable String commentId) throws Exception {
        service.delete(shortsId, commentId, uid());
    }

    @PostMapping("/{commentId}/like")
    public void like(@PathVariable String shortsId, @PathVariable String commentId) throws Exception {
        service.like(shortsId, commentId, uid());
    }

    @DeleteMapping("/{commentId}/like")
    public void unlike(@PathVariable String shortsId, @PathVariable String commentId) throws Exception {
        service.unlike(shortsId, commentId, uid());
    }
}