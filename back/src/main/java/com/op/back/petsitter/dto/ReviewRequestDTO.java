package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewRequestDTO {
    private String reservationId;
    private int rating;
    private String content;
    private String nickname;
}
