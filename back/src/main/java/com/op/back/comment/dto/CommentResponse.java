package com.op.back.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CommentResponse {
    private String id;
    private String parentId;
    private String uid;
    private String content;
    private long likeCount;
    private boolean liked;
    private Instant createdAt;
}
