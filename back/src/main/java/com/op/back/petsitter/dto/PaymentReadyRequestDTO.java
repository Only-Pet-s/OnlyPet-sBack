package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentReadyRequestDTO {
    private String reservationId;
}
