package com.op.back.lecture.search;

import java.util.List;
import lombok.Data;

@Data
public class LectureSearchDocument {
    private String lectureId;
    private String title;
    private String description;
    private List<String> tags;
    private String category;
    private String lecturerUid;
    private String lecturerName;
    private double rating;
    private int price;
    private boolean adminApproved;
    private boolean published;
}