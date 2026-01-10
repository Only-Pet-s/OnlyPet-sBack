package com.op.back.payment.dto;

import com.op.back.payment.model.PurchaseStatus;
import com.op.back.payment.model.PurchaseType;
import com.op.back.payment.model.SubscriptionPlan;

public record PurchaseResponse(
        String purchaseId,
        PurchaseType type,
        String lectureId,
        SubscriptionPlan subscriptionPlan,
        String targetLecturerUid,
        int price,
        String currency,
        PurchaseStatus status,
        String purchasedAt,
        String expiredAt
) {}
