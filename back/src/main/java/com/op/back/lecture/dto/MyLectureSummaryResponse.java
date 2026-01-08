package com.op.back.lecture.dto;

import java.util.List;

public record MyLectureSummaryResponse(
        int studyingCount,
        int completedCount,
        int averageProgress,
        int totalStudyMinutes,
        List<MyLectureItemResponse> lectures
) {
}
