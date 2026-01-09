package com.op.back.payment.model;

import com.google.cloud.Timestamp;
import lombok.Data;

@Data
public class Purchase {
    private String purchaseId;
    private PurchaseType type;

    // LECTURE 구매일 때만
    private String lectureId;

    // SUBSCRIPTION일 때만
    private SubscriptionPlan subscriptionPlan;
    private String targetLecturerUid; // LECTURER 플랜 대상

    private int price;
    private String currency; // KRW

    private PaymentMethod paymentMethod;
    private String paymentKey; // PG 트랜잭션 ID 등

    private PurchaseStatus status;

    private Timestamp purchasedAt;
    private Timestamp expiredAt; // 구독인 경우 만료
}
