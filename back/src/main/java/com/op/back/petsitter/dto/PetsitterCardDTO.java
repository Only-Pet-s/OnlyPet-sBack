package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetsitterCardDTO {
    private String petsitterId; // 펫 시터 아이디
    private String name; // 펫 시터 이름
    private String profileImageUrl; // 펫 시터 프로필 사진

    private String address; // 펫 시터 주소
    private double distance; // 거리
    private double rating; // 평점
    private double mannerTemp; // 매너 온도

    private String caption; // 펫 시터의 설명글

    private boolean isVerified; // 인증 여부(자격증)

    private int career; // 경력 년수
    private int completeCount; // 완료 횟수
    private int responseRatio; // 응답률
    private int price; // 가격

    private boolean reserveAvailable; // 예약 가능 여부
    private boolean dog; // 강아지 가능 여부
    private boolean cat; // 고양이 가능 여부
    private boolean etc; // 기타 동물
}
