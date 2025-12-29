package com.op.back.petsitter.dto;

// 펫시터 정보 변경 DTO

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetsitterUpdateRequestDTO {
    private String address;
    private String caption;
    private Integer career;
    private Integer price;

    private Boolean dog;
    private Boolean cat;
    private Boolean etc;

    private Boolean reserveAvailable;
}
