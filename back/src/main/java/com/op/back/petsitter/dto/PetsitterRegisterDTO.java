package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// db 저장용 dto
@Getter
@AllArgsConstructor
public class PetsitterRegisterDTO {
    private String petsitterId;
    private String name;
    private String profileImageUrl;
    private String address;
    private double lat;
    private double lng;

    private String caption;  // 소개글

    private int career;      // 경력
    private int price;       // 가격

    private boolean dog;
    private boolean cat;
    private boolean etc;
}
