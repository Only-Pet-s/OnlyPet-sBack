package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReadUserReservationDTO {
    private String reservationId;
    private String petsitterId;
    private String petsitterName;
    private String petsitterProfileImage;
    private String date;
    private String startTime;
    private String endTime;
    private String reservationStatus;
    private String address;
    private String phone;
    private Long price;
    private List<PetInfoDTO> pets;
    private String requestNote;
    private Double mannerTemp;
    private Double rating;
    private String careType;
    private Boolean canReview;
}
