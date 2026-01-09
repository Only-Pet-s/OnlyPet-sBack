package com.op.back.payment.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.Firestore;
import com.op.back.payment.model.Purchase;
import com.op.back.payment.model.PurchaseStatus;
import com.op.back.payment.model.PurchaseType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private final Firestore firestore;

    @Override
    public void save(String uid, Purchase purchase) {
        try {
            firestore.collection("users")
                    .document(uid)
                    .collection("purchases")
                    .document(purchase.getPurchaseId())
                    .set(purchase)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("구매 저장 실패", e);
        }
    }

    @Override
    public Optional<Purchase> findById(String uid, String purchaseId) {
        try {
            var doc = firestore.collection("users")
                    .document(uid)
                    .collection("purchases")
                    .document(purchaseId)
                    .get()
                    .get();
            return doc.exists() ? Optional.ofNullable(doc.toObject(Purchase.class)) : Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("구매 조회 실패", e);
        }
    }

    @Override
    public List<Purchase> findAll(String uid) {
        try {
            return firestore.collection("users")
                    .document(uid)
                    .collection("purchases")
                    .orderBy("purchasedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(d -> d.toObject(Purchase.class))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("구매 목록 조회 실패", e);
        }
    }

    @Override
    public boolean existsLecturePurchase(String uid, String lectureId) {
        try {
            var qs = firestore.collection("users")
                    .document(uid)
                    .collection("purchases")
                    .whereEqualTo("type", PurchaseType.LECTURE)
                    .whereEqualTo("lectureId", lectureId)
                    .whereEqualTo("status", PurchaseStatus.PAID)
                    .limit(1)
                    .get()
                    .get();
            return !qs.isEmpty();
        } catch (Exception e) {
            throw new RuntimeException("구매 여부 조회 실패", e);
        }
    }
}
