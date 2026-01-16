package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReadPetsitterReservedDTO {
    private String reservationId;
    private String userUid;
    private String userName;
    private String userProfileImage;
    private String phone;
    private String address;
    private Long price;
    private String careType;
    private String date;
    private String startTime;
    private String endTime;
    private List<PetInfoDTO> pets;
    private String requestNote;
    private String reservationStatus;
}
