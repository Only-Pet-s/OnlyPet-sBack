package com.op.back.payment.dto;

import com.op.back.payment.model.SubscriptionPlan;

public record SubscriptionResponse(
        String subscriptionId,
        SubscriptionPlan plan,
        String targetLecturerUid,
        boolean active,
        String startedAt,
        String expiredAt
) {}
