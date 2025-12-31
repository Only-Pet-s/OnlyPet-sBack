package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadPetsitterReservedDTO {
    private String reservationId;
    private String userUid;
    private String userName;
    private String userProfileImage;
    private String phone;
    private String address;
    private String date;
    private String startTime;
    private String endTime;
    private String petType;
    private String petName;
    private String reservationStatus;
}
