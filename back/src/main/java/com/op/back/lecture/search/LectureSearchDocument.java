package com.op.back.lecture.search;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Data;

@Document(indexName = "lecture-index",createIndex = false)
@Data
public class LectureSearchDocument {
    @Id
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