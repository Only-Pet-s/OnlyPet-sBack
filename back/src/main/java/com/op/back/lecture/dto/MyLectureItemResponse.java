package com.op.back.lecture.dto;

public record MyLectureItemResponse(
        String lectureId,
        String title,
        String thumbnailUrl,
        String lecturerName,
        int progressPercent,
        boolean completed,
        int totalWatchedSeconds,
        String lastWatchedAt
) {
}
