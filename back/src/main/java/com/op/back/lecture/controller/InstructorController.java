package com.op.back.lecture.controller;

import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.model.Instructor;
import com.op.back.lecture.service.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    private String currentUid() {
        return (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // 강사 등록
    @PostMapping
    public void register(
            @RequestBody InstructorCreateRequest req
    ) {
        instructorService.registerInstructor(req, currentUid());
    }

    // 강사 정보 조회
    @GetMapping("/{instructorUid}")
    public Instructor get(
            @PathVariable String instructorUid
    ) {
        return instructorService.getInstructor(instructorUid);
    }
}
