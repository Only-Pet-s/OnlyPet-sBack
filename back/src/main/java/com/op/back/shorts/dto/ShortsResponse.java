package com.op.back.shorts.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ShortsResponse {
    // 식별자
    private String id;

    // 작성자
    private String uid;
    private String nickname;

    // 미디어
    private String mediaUrl;
    private String thumbnailUrl;

    // 본문
    private String description;
    private List<String> hashtags;

    // 카운트
    private long likeCount;
    private long commentCount;
    private long viewCount;

    // 댓글 허용 여부
    private boolean commentAvailable;

    // 상태
    private boolean liked;
    private boolean bookmarked;
    private boolean mine;

    // 시간
    private Instant createdAt;
}
