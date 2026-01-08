package com.op.back.payment.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.google.cloud.firestore.Firestore;
import com.op.back.payment.model.Subscription;
import com.op.back.payment.model.SubscriptionPlan;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final Firestore firestore;

    @Override
    public void save(String uid, Subscription subscription) {
        try {
            firestore.collection("users")
                    .document(uid)
                    .collection("subscriptions")
                    .document(subscription.getSubscriptionId())
                    .set(subscription)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("구독 저장 실패", e);
        }
    }

    @Override
    public Optional<Subscription> findActiveAllInOne(String uid) {
        try {
            var now = com.google.cloud.Timestamp.now();
            var qs = firestore.collection("users")
                    .document(uid)
                    .collection("subscriptions")
                    .whereEqualTo("plan", SubscriptionPlan.ALL_IN_ONE)
                    .whereEqualTo("active", true)
                    .whereGreaterThan("expiredAt", now)
                    .limit(1)
                    .get()
                    .get();
            if (qs.isEmpty()) return Optional.empty();
            return Optional.ofNullable(qs.getDocuments().get(0).toObject(Subscription.class));
        } catch (Exception e) {
            throw new RuntimeException("구독 조회 실패", e);
        }
    }

    @Override
    public Optional<Subscription> findActiveLecturer(String uid, String lecturerUid) {
        try {
            var now = com.google.cloud.Timestamp.now();
            var qs = firestore.collection("users")
                    .document(uid)
                    .collection("subscriptions")
                    .whereEqualTo("plan", SubscriptionPlan.LECTURER)
                    .whereEqualTo("active", true)
                    .whereEqualTo("targetLecturerUid", lecturerUid)
                    .whereGreaterThan("expiredAt", now)
                    .limit(1)
                    .get()
                    .get();
            if (qs.isEmpty()) return Optional.empty();
            return Optional.ofNullable(qs.getDocuments().get(0).toObject(Subscription.class));
        } catch (Exception e) {
            throw new RuntimeException("구독 조회 실패", e);
        }
    }

    @Override
    public List<Subscription> findAll(String uid) {
        try {
            return firestore.collection("users")
                    .document(uid)
                    .collection("subscriptions")
                    .orderBy("startedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(d -> d.toObject(Subscription.class))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("구독 목록 조회 실패", e);
        }
    }
}
