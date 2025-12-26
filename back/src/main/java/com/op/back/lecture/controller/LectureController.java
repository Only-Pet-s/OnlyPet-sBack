package com.op.back.lecture.controller;

import com.op.back.lecture.dto.LectureCreateRequest;
import com.op.back.lecture.dto.LectureDetailResponse;
import com.op.back.lecture.dto.LectureListItemResponse;
import com.op.back.lecture.service.LectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    private String currentUid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 1) 강의 등록 (강의자만 가능: 서비스에서 체크)
    @PostMapping
    public String create(@RequestBody LectureCreateRequest request) {
        return lectureService.createLecture(request, currentUid());
    }

    // 2) 강의 전체 목록
    @GetMapping
    public List<LectureListItemResponse> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return lectureService.getLectures(limit, offset);
    }

    // 3) 강의 상세
    @GetMapping("/{lectureId}")
    public LectureDetailResponse detail(@PathVariable String lectureId) {
        return lectureService.getLecture(lectureId);
    }

    // 4) 특정 강의자 강의 목록
    @GetMapping("/by-lecturer/{lecturerUid}")
    public List<LectureListItemResponse> byLecturer(
            @PathVariable String lecturerUid,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return lectureService.getLecturesByLecturer(lecturerUid, limit, offset);
    }

    //검색
    @GetMapping("/search")
    public List<LectureListItemResponse> searchLectures(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return lectureService.searchLectures(keyword, tags, category, limit, offset);
    }
}
