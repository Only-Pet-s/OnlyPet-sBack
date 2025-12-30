package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelReservationResponseDTO {
    private String reservationId;
    private Integer price;
    private Integer fee;
    private Integer refundAmount;
}
