package com.op.back.lecture.dto;

import java.util.List;

/**
 강사 본인이 수정 가능한 필드만 허용 (부분 수정)
 */
public record InstructorUpdateRequest(
        String name,
        String profileImageUrl,
        String intro,
        Integer careerYears,
        List<String> specialty
) {}
