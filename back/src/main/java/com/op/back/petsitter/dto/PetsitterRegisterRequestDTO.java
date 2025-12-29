package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 사용자 요청 용 dto
@Getter
@AllArgsConstructor
public class PetsitterRegisterRequestDTO {
    private String petsitterId;
    private String name;
    private String address;
    private String phone;

    private String caption;  // 소개글

    private int career;      // 경력
    private int price;       // 가격

    private boolean dog;
    private boolean cat;
    private boolean etc;
}
