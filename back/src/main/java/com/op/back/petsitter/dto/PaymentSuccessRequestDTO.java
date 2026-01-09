package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentSuccessRequestDTO {
    private String reservationId;
    private String paymentId;
}
