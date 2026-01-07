package com.op.back.lecture.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.op.back.lecture.dto.LectureReviewResponse;
import com.op.back.lecture.service.LectureReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lecture/my")
@RequiredArgsConstructor
public class MyLectureReviewController {

    private final LectureReviewService lectureReviewService;

    private String currentUid() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    @GetMapping("/reviews")
    public List<LectureReviewResponse> myReviews() {
        return lectureReviewService.getMyReviews(currentUid());
    }
}
