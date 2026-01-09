package com.op.back.myPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class MyPageLikeDTO {
    private String type;
    private String id;
    private String mediaType;
    private String mediaUrl;
    private String likedAt;
}
