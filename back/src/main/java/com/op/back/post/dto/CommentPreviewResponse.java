package com.op.back.post.dto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentPreviewResponse {
    private String id;
    private String uid;
    private String content;
}
