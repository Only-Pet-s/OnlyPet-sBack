package com.op.back.post.dto;

import java.util.List;

import com.google.auto.value.AutoValue.Builder;

import lombok.Getter;


@Getter
@Builder
public class PostCreateRequest {
    //게시글 텍스트 내용
    private String content;
    //해시태그 목록
    private List<String> hashtags;
    //공개범위
    private Boolean commentAvailable;
}
