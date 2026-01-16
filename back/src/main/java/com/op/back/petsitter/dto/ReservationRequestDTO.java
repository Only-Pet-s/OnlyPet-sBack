package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReservationRequestDTO {

    private String petsitterId;

    // yyyy-MM-dd
    private String date;

    // HH:mm
    private String startTime;
    private String endTime;

    private String phone;
    private String address;

    private String careType; // VISIT / CONSIGN?
    private List<PetInfoDTO> pets;

    private String requestNote;

//    private Integer price;
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
