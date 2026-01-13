package com.op.back.petsitter.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetsitterReviewResponseDTO {
    private String reviewId;
    private String userUid;
    private String nickname;
    private int rating;
    private String content;
    private Timestamp createdAt;
}
