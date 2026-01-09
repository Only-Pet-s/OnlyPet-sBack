package com.op.back.payment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.payment.dto.LecturePurchaseRequest;
import com.op.back.payment.dto.PurchaseResponse;
import com.op.back.payment.model.Purchase;
import com.op.back.payment.model.PurchaseStatus;
import com.op.back.payment.model.PurchaseType;
import com.op.back.payment.repository.PurchaseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final LectureRepository lectureRepository;

    //강의 구매하기
    @Override
    public PurchaseResponse purchaseLecture(String uid, LecturePurchaseRequest request) {
        var lecture = lectureRepository.findById(request.lectureId())
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        Purchase p = new Purchase();
        p.setPurchaseId(UUID.randomUUID().toString());
        p.setType(PurchaseType.LECTURE);
        p.setLectureId(request.lectureId());
        p.setPrice(lecture.getPrice());
        p.setCurrency("KRW");
        p.setPaymentMethod(request.paymentMethod());
        p.setPaymentKey("MOCK-" + p.getPurchaseId());
        p.setStatus(PurchaseStatus.PAID);
        p.setPurchasedAt(Timestamp.now());

        purchaseRepository.save(uid, p);
        return toResponse(p);
    }

    //access에서 사용
    @Override
    public boolean hasLecturePurchase(String uid, String lectureId) {
        return purchaseRepository.existsLecturePurchase(uid, lectureId);
    }

    //내가 구매한 내역
    @Override
    public List<PurchaseResponse> getMyPurchases(String uid) {
        return purchaseRepository.findAll(uid).stream().map(this::toResponse).toList();
    }

    private PurchaseResponse toResponse(Purchase p) {
        return new PurchaseResponse(
                p.getPurchaseId(),
                p.getType(),
                p.getLectureId(),
                p.getSubscriptionPlan(),
                p.getTargetLecturerUid(),
                p.getPrice(),
                p.getCurrency(),
                p.getStatus(),
                p.getPurchasedAt() != null ? p.getPurchasedAt().toDate().toInstant().toString() : null,
                p.getExpiredAt() != null ? p.getExpiredAt().toDate().toInstant().toString() : null
        );
    }
}
