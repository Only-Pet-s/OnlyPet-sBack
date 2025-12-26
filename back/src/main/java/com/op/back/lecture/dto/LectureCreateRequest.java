package com.op.back.lecture.dto;

import java.util.List;

public record LectureCreateRequest(
    String title,
    String description,
    String category,
    int price,
    String thumbnailUrl,
    List<String> tags,
    boolean adminApproved
){}