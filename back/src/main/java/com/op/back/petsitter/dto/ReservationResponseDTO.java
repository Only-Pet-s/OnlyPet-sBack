package com.op.back.petsitter.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationResponseDTO {

    private String reservationId;
    private String reservationStatus;
    private Timestamp paymentExpireAt;
}
