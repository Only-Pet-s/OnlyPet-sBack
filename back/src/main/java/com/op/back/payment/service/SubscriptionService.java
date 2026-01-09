package com.op.back.payment.service;

import java.util.List;

import com.op.back.lecture.model.Lecture;
import com.op.back.payment.dto.SubscriptionPurchaseRequest;
import com.op.back.payment.dto.SubscriptionResponse;

public interface SubscriptionService {
    SubscriptionResponse subscribe(String uid, SubscriptionPurchaseRequest request);
    boolean hasValidAccess(String uid, Lecture lecture);
    List<SubscriptionResponse> getMySubscriptions(String uid);
}
