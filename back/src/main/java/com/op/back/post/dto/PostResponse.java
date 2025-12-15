package com.op.back.post.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class PostResponse {
    // 식별자
    private String id;

    // 작성자 (uid만)
    private String uid;
    private String nickname;

    // 본문
    private String content;
    private String mediaUrl;
    private String mediaType;

    // 옵션
    private boolean commentAvailable;

    // 해시태그
    private List<String> hashtags;

    // 카운트
    private long likeCount;
    private long commentCount;
    private long viewCount;

    // 현재 로그인 사용자 기준 상태
    private boolean liked;
    private boolean bookmarked;
    private boolean mine;


    // 시간
    private Instant createdAt;
}
