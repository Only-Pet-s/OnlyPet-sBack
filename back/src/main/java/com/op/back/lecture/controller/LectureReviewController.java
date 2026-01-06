package com.op.back.lecture.controller;

import com.op.back.lecture.dto.LectureReviewCreateRequest;
import com.op.back.lecture.dto.LectureReviewListResponse;
import com.op.back.lecture.dto.LectureReviewResponse;
import com.op.back.lecture.dto.LectureReviewUpdateRequest;
import com.op.back.lecture.service.LectureReviewService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lectures/{lectureId}/reviews")
@RequiredArgsConstructor
public class LectureReviewController {

    private final LectureReviewService lectureReviewService;

    private String currentUid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public LectureReviewResponse create(
            @PathVariable String lectureId,
            @RequestBody LectureReviewCreateRequest req
    ) {
        return lectureReviewService.create(lectureId, req, currentUid());
    }

    @GetMapping
    public LectureReviewListResponse list(
            @PathVariable String lectureId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return lectureReviewService.list(lectureId, limit, offset, currentUid());
    }

    @GetMapping("/me")
    public LectureReviewResponse mine(@PathVariable String lectureId) {
        return lectureReviewService.getMine(lectureId, currentUid());
    }

    @PutMapping
    public LectureReviewResponse update(
            @PathVariable String lectureId,
            @RequestBody LectureReviewUpdateRequest req
    ) {
        return lectureReviewService.update(lectureId, req, currentUid());
    }

    @DeleteMapping
    public void delete(@PathVariable String lectureId) {
        lectureReviewService.delete(lectureId, currentUid());
    }
}
