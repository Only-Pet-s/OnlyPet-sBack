package com.op.back.payment.dto;

import com.op.back.payment.model.PaymentMethod;
import com.op.back.payment.model.SubscriptionPlan;

public record SubscriptionPurchaseRequest(
        SubscriptionPlan plan,
        String targetLecturerUid, // LECTURER 플랜일 때만 사용
        int months,               // 몇 개월 결제 (기본 1)
        PaymentMethod paymentMethod
) {}
