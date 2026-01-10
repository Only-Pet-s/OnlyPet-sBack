package com.op.back.petsitter.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentReadyResponseDTO {
    private String paymentId;
    private Integer amount;
    private Timestamp expireAt;
}
