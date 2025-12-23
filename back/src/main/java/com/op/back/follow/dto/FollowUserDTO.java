package com.op.back.follow.dto;

// 팔로잉 또는 팔로워 조회/등록 용 DTO

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowUserDTO {
    private String uid;
    private String nickname;
    private String profileImage;
}
