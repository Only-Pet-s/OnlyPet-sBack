package com.op.back.payment.model;

import com.google.cloud.Timestamp;
import lombok.Data;

@Data
public class Subscription {
    private String subscriptionId;
    private SubscriptionPlan plan;

    // LECTURER 플랜 대상
    private String targetLecturerUid;

    private boolean active;
    private Timestamp startedAt;
    private Timestamp expiredAt;
}
