package com.op.back.shorts.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ShortsResponse {
    private String id;
    private String uid;
    private String mediaUrl;
    private String thumbnailUrl;
    private String description;
    private List<String> hashtags;

    private long likeCount;
    private long commentCount;
    private long viewCount;

    private boolean liked;
    private boolean bookmarked;

    private Instant createdAt;
}
