package com.op.back.lecture.model;

import com.google.cloud.Timestamp;
import lombok.Data;

@Data
public class UserLecture {
    private String lectureId;

    private Timestamp enrolledAt;
    private int progressPercent; // 0~100
    private boolean completed;
    private int totalWatchedSeconds;
    private Timestamp lastWatchedAt;
}
