package com.op.back.post.dto;

import lombok.Data;

@Data
public class PostMediaReorderRequest {
    private String mediaId;
    private Integer order;
}
