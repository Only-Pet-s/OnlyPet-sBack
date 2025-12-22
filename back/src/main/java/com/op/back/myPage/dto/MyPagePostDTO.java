package com.op.back.myPage.dto;

import com.google.cloud.firestore.DocumentSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPagePostDTO {
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
