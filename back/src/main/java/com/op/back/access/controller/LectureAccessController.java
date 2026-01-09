package com.op.back.access.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.op.back.access.LectureAccessResult;
import com.op.back.access.LectureAccessService;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.repository.LectureRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lectures/access")
@RequiredArgsConstructor
public class LectureAccessController {

    private final LectureRepository lectureRepository;
    private final LectureAccessService lectureAccessService;

    private String currentUidOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        return (p instanceof String s) ? s : null;
    }

    @GetMapping("/{lectureId}")
    public LectureAccessResult check(@PathVariable String lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));
        return lectureAccessService.check(currentUidOrNull(), lecture);
    }
}
