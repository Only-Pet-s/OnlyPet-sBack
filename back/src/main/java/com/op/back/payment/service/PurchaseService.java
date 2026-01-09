package com.op.back.payment.service;

import java.util.List;

import com.op.back.payment.dto.LecturePurchaseRequest;
import com.op.back.payment.dto.PurchaseResponse;

public interface PurchaseService {
    PurchaseResponse purchaseLecture(String uid, LecturePurchaseRequest request);
    boolean hasLecturePurchase(String uid, String lectureId);
    List<PurchaseResponse> getMyPurchases(String uid);
}
