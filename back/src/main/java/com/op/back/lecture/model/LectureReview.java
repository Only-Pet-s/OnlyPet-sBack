package com.op.back.lecture.model;

import com.google.cloud.Timestamp;
import lombok.Data;

@Data
public class LectureReview {
    private String uid;
    private String nickname;
    private double rating;
    private String content;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
