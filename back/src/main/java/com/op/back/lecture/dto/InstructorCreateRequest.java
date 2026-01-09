package com.op.back.lecture.dto;

import java.util.List;

public record InstructorCreateRequest(
        String name,
        String intro,
        int careerYears,
        List<String> specialty
) {}