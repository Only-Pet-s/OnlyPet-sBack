package com.op.back.post.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostUpdateRequest {

    // 수정 가능 항목만
    private String content;
    private List<String> hashtags;
    private Boolean commentAvailable;

    // media 교체 허용할 거면 사용
    private String mediaType;
}
