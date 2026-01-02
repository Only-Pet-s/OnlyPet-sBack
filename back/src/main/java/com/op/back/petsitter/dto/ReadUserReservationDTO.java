package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
}
