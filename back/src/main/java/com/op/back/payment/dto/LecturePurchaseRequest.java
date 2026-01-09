package com.op.back.payment.dto;

import com.op.back.payment.model.PaymentMethod;

public record LecturePurchaseRequest(
        String lectureId,
        PaymentMethod paymentMethod
) {}
