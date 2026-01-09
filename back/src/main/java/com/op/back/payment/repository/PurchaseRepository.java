package com.op.back.payment.repository;

import java.util.List;
import java.util.Optional;

import com.op.back.payment.model.Purchase;

public interface PurchaseRepository {
    void save(String uid, Purchase purchase);
    Optional<Purchase> findById(String uid, String purchaseId);
    List<Purchase> findAll(String uid);
    boolean existsLecturePurchase(String uid, String lectureId);
}
