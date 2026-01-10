package com.op.back.shorts.model;

import com.google.cloud.Timestamp;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Shorts {

    private String id;               // Firestore 문서 ID
    private String uid;              // 작성자 UID
    private String mediaUrl;         // 영상 URL
    private String thumbnailUrl;     // 썸네일 URL (추후 기능)
    private String description;      // 설명/텍스트
    private List<String> hashtags;

    private Long likeCount;
    private Long commentCount;
    private Long viewCount;

    private Boolean commentAvailable;

    private Timestamp createdAt;     // Firestore Timestamp
}