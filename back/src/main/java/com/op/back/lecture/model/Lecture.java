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
    private double rating;
    private int reviewCount;

    private String thumbnailUrl;

    private Timestamp createdAt;
}
