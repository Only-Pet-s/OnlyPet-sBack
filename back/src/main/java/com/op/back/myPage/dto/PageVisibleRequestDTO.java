package com.op.back.myPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageVisibleRequestDTO { // 마이페이지 공개 범위 변경용 DTO
    private String pageVisible;
}
