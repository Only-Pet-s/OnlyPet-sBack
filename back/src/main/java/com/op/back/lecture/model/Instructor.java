package com.op.back.lecture.model;

import java.util.List;

import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Instructor {
    private String instructorUid;
    private String name;
    private String profileImageUrl;
    private String intro;
    private int careerYears;
    private List<String> specialty;

    private int lectureCount;
    private int totalStudents;
    private int totalPurchases;
    private double averageRating;
    private int reviewCount;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
