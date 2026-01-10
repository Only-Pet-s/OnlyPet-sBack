package com.op.back.payment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.lecture.model.Lecture;
import com.op.back.payment.dto.SubscriptionPurchaseRequest;
import com.op.back.payment.dto.SubscriptionResponse;
import com.op.back.payment.model.Subscription;
import com.op.back.payment.model.SubscriptionPlan;
import com.op.back.payment.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public SubscriptionResponse subscribe(String uid, SubscriptionPurchaseRequest request) {
        int months = request.months() <= 0 ? 1 : request.months();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(60L * 60 * 24 * 30 * months); // 단순 30일 * months

        Subscription s = new Subscription();
        s.setSubscriptionId(UUID.randomUUID().toString());
        s.setPlan(request.plan());
        s.setTargetLecturerUid(request.targetLecturerUid());
        s.setActive(true);
        s.setStartedAt(Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano()));
        s.setExpiredAt(Timestamp.ofTimeSecondsAndNanos(exp.getEpochSecond(), exp.getNano()));

        subscriptionRepository.save(uid, s);
        return toResponse(s);
    }

    @Override
    public boolean hasValidAccess(String uid, Lecture lecture) {
        // 올인원
        if (subscriptionRepository.findActiveAllInOne(uid).isPresent()) return true;

        // 강의자 구독: 해당 강의자의 강의만
        String lecturerUid = lecture.getLecturerUid();
        if (lecturerUid != null && subscriptionRepository.findActiveLecturer(uid, lecturerUid).isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    public List<SubscriptionResponse> getMySubscriptions(String uid) {
        return subscriptionRepository.findAll(uid).stream().map(this::toResponse).toList();
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getSubscriptionId(),
                s.getPlan(),
                s.getTargetLecturerUid(),
                s.isActive(),
                s.getStartedAt() != null ? s.getStartedAt().toDate().toInstant().toString() : null,
                s.getExpiredAt() != null ? s.getExpiredAt().toDate().toInstant().toString() : null
        );
    }
}
