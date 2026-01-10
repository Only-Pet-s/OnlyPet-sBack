package com.op.back.lecture.model;

import lombok.Data;
import java.util.List;
import com.google.cloud.Timestamp;

@Data
public class Lecture {

    // Theme 정보
    private String lectureId;          // == themeId
    private String title;
    private String description;
    private String category;
    private List<String> tags;

    private int price;

    // 강의자
    private String lecturerUid;
    private String lecturerName;

    // 상태
    private boolean adminApproved;
    private boolean published;

    //  집계
    private int videoCount;
    // 총 강의 시간(분) - 테마(강의) 카드용. 없으면 서비스에서 계산해 set 할 수 있음.
    private int totalDurationMinutes;
    private double rating;
    private int reviewCount;

    // 난이도/학습 목표(옵션)
    private String difficulty; // BEGINNER / INTERMEDIATE / ADVANCED
    private List<String> learningObjectives;

    private String thumbnailUrl;

    private Timestamp createdAt;
}
