package com.op.back.lecture.dto;

import java.util.List;

public record InstructorResponse(
    String instructorUid,
    String name,
    String intro,
    int careerYears,
    List<String> specialty,
    int lectureCount,
    int totalStudents,
    int totalPurchases,
    double averageRating,
    int reviewCount,
    String createdAt,
    String updatedAt
) {}
