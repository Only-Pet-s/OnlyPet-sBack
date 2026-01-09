package com.op.back.myPage.dto;

import com.google.cloud.firestore.DocumentSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageShortDTO {
    private String shortId;
    private String mediaUrl;
    private String thumbnailUrl;

    public static MyPageShortDTO from(DocumentSnapshot doc) {
        return new MyPageShortDTO(
                doc.getId(),
                doc.getString("mediaUrl"),
                doc.getString("thumbnailUrl")
        );
    }
}
