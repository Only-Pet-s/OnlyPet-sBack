package com.op.back.comment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {
    private String content;
    private String parentId;
}
