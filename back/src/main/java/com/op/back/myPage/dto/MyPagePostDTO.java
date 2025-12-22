package com.op.back.myPage.dto;

import com.google.cloud.firestore.DocumentSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPagePostDTO { // 마이페이지 밑에 게시글 리스트 용 DTO, 내가 올린 영상 피드, 좋아요 추가 필요
    private String postId;
    private String mediaType;
    private String mediaUrl;

    public static MyPagePostDTO from(DocumentSnapshot doc) {
        return new MyPagePostDTO(
                doc.getId(),
                doc.getString("mediaType"),
                doc.getString("mediaUrl")
        );
    }
}
