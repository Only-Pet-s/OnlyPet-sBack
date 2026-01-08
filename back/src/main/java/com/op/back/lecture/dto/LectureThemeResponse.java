package com.op.back.lecture.dto;

import java.time.Instant;

public record LectureThemeResponse(
        String themeId,
        String title,
        int order,
        Instant createdAt
) {}
