package com.op.back.lecture.controller;

import com.op.back.lecture.dto.LectureProgressUpdateRequest;
import com.op.back.lecture.dto.LectureReviewResponse;
import com.op.back.lecture.dto.MyLectureSummaryResponse;
import com.op.back.lecture.service.LectureReviewService;
import com.op.back.lecture.service.MyLectureService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class MyLectureController {

    private final MyLectureService myLectureService;
    private final LectureReviewService lectureReviewService;

    private String currentUid() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    // 수강 시작/등록
    @PostMapping("/{lectureId}/enroll")
    public void enroll(@PathVariable String lectureId) {
        myLectureService.enroll(lectureId, currentUid());
    }

    // 내 학습(요약 + 진행중/완강 리스트)
    @GetMapping("/my")
    public MyLectureSummaryResponse myLectures() {
        return myLectureService.getMyLectures(currentUid());
    }

    // 진도 업데이트
    @PostMapping("/{lectureId}/progress")
    public void updateProgress(
            @PathVariable String lectureId,
            @RequestBody LectureProgressUpdateRequest request
    ) {
        myLectureService.updateProgress(lectureId, currentUid(), request);
    }


    @GetMapping("my/reviews")
    public List<LectureReviewResponse> myReviews() {
        return lectureReviewService.getMyReviews(currentUid());
    }
}
