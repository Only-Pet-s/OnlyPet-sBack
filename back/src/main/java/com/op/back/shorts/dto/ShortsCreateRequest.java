package com.op.back.shorts.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShortsCreateRequest {
    private String description;
    private List<String> hashtags;

    //댓글 허용 여부 (기본: true)
    private Boolean commentAvailable;
}
