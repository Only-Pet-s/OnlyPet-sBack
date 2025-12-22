package com.op.back.myPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageDTO {
    private String nickname;
    private String profileImageUrl;
    private String captionTitle;
    private String captionContent;
    private Long postCount;
    private Long followerCount;
    private Long followingCount;

    private boolean isOwner;
}
