package com.op.back.lecture.dto;

//특정 강의 영상목록
public record LectureVideoResponse(
        String videoId,
        String title,
        String description,
        int order,
        int duration,
        String videoUrl,
        String thumbnailUrl,
        boolean preview,
        boolean purchased,
        boolean deleted,
        String createdAt
) {}