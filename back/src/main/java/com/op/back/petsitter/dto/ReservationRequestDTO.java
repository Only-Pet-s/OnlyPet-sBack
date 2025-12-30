package com.op.back.petsitter.dto;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationRequestDTO {

    private String petsitterId;

    // yyyy-MM-dd
    private String date;

    // HH:mm
    private String startTime;
    private String endTime;

    private String careType; // VISIT / CONSIGN?
    private String petType;  // DOG / CAT / ETC
    private String petName;

    private String requestNote;

    private Integer price;
//    private String uid;
//    private String petsitterId;
//
//    private Date date;
//    private String startTime;
//    private String endTime;
//
//    private String reservationStatus; // HOLD, RESERVED
//    private String paymentStatus;
//
//    private Integer price;
//
//    private Timestamp createdAt;
//    private Timestamp timestamp;
}
