package com.op.back.post.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostMediaResponse {
    private String id;
    private String type; // IMAGE | VIDEO
    private String mediaUrl;
    private int order;
}
