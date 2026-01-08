package com.op.back.payment.repository;

import java.util.List;
import java.util.Optional;

import com.op.back.payment.model.Subscription;

public interface SubscriptionRepository {
    void save(String uid, Subscription subscription);
    Optional<Subscription> findActiveAllInOne(String uid);
    Optional<Subscription> findActiveLecturer(String uid, String lecturerUid);
    List<Subscription> findAll(String uid);
}
