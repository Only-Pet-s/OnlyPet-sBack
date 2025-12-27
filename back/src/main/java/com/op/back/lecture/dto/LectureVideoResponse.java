package com.op.back.lecture.dto;

import java.time.Instant;

public record LectureVideoResponse(
        String videoId,
        String title,
        String description,
        int order,
        String videoUrl,
        boolean preview,
        boolean purchased,
        boolean deleted,
        Instant createdAt
) {}