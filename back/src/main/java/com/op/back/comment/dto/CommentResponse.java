package com.op.back.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class CommentResponse {
    private String id;
    private String parentId;

    private String uid;
    private String nickname;

    private String content;
    private long likeCount;
    private boolean liked;
    
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;

    private List<CommentResponse> children;
}
