package com.op.back.lecture.model;

import lombok.Data;

import com.google.cloud.Timestamp;

@Data
public class LectureVideo {

    private String videoId;
    private String lectureId;       //어떤 테마 소속인지

    private String title;
    private String description;

    private int order;              // 강의 순서
    private String videoUrl;
    private int duration;           // 초 단위

    private boolean preview;
    private String thumbnailUrl;
    private Timestamp createdAt;

    private boolean purchased;
    private boolean deleted;
}