package com.op.back.lecture.dto;

public record LectureProgressUpdateRequest(
        Integer progressPercent,
        Integer watchedSecondsDelta
) {
}
