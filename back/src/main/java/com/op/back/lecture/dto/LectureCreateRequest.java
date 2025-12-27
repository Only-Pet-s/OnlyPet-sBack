package com.op.back.lecture.dto;

import java.util.List;

//테마 데이터만 가짐
public record LectureCreateRequest(
        String title,
        String description,
        String category,
        List<String> tags,
        int price
) {}