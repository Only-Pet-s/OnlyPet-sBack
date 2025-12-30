package com.op.back.petsitter.entity;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationEntity {

    private String userUid;
    private String petsitterId;

    private String date;
    private String startTime;
    private String endTime;

    private String reservationStatus; // HOLD / RESERVED / CANCELED / COMPLETED
    private String paymentStatus;      // PENDING / COMPLETED / CANCELED

    private Integer price;

    private Timestamp createdAt;
    private Timestamp paymentExpireAt;
}
