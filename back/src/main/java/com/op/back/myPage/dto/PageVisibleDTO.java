package com.op.back.myPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageVisibleDTO { // 기존 마이 페이지 공개 상태 조회용 DTO
    private String pageVisible;
}
