package com.op.back.lecture.dto;

import java.util.List;
import java.time.Instant;

public record LectureDetailResponse (
    String lectureId,
    String title,
    String description,
    String category,
    int price,
    String thumbnailUrl,
    String lecturerUid,
    String lecturerName,
    boolean adminApproved,
    boolean published,
    List<String> tags,
    double rating,
    int reviewCount,
    Instant createdAt
){}
