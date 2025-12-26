package com.op.back.lecture.model;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Lecture {
    private String lectureId;
    private String title;
    private String description;
    private String category;
    private int price;
    private String thumbnailUrl;

    private String lecturerUid;
    private String lecturerName;

    private boolean adminApproved;
    private boolean published;

    private List<String> tags;

    private double rating;
    private int reviewCount;

    private Instant createdAt;
}
