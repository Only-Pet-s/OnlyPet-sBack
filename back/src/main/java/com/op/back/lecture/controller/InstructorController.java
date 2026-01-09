package com.op.back.lecture.controller;

import com.op.back.lecture.dto.InstructorCreateRequest;
import com.op.back.lecture.dto.InstructorResponse;
import com.op.back.lecture.dto.InstructorUpdateRequest;
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
    public void register(@RequestBody InstructorCreateRequest req) {
        instructorService.registerInstructor(currentUid(), req);
    }

    // 강사 정보 수정
    @PutMapping("/me")
    public InstructorResponse updateMe(@RequestBody InstructorUpdateRequest req) {
        Instructor instructor =
                instructorService.updateMyInstructor(currentUid(), req);
        return toResponse(instructor);
    }

    // 강사 정보 조회
    @GetMapping("/{instructorUid}")
    public InstructorResponse get(@PathVariable String instructorUid) {
        Instructor instructor =
                instructorService.getInstructor(instructorUid);
        return toResponse(instructor);
    }
    
    //DTO 변환 메서드
    private InstructorResponse toResponse(Instructor i) {
        return new InstructorResponse(
                i.getInstructorUid(),
                i.getName(),
                i.getIntro(),
                i.getCareerYears(),
                i.getSpecialty(),
                i.getLectureCount(),
                i.getTotalStudents(),
                i.getTotalPurchases(),
                i.getAverageRating(),
                i.getReviewCount(),
                i.getCreatedAt() != null
                        ? i.getCreatedAt().toDate().toInstant().toString()
                        : null,
                i.getUpdatedAt() != null
                        ? i.getUpdatedAt().toDate().toInstant().toString()
                        : null
        );
    }
}
