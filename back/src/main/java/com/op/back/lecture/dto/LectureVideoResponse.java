package com.op.back.lecture.dto;

import java.time.Instant;

//특정 강의 영상목록
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