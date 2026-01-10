package com.op.back.access;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.op.back.lecture.model.Lecture;
import com.op.back.payment.service.PurchaseService;
import com.op.back.payment.service.SubscriptionService;

@Service
@RequiredArgsConstructor
public class LectureAccessService {

    private final PurchaseService purchaseService;
    private final SubscriptionService subscriptionService;

    public LectureAccessResult check(String uid, Lecture lecture) {
        // 비로그인: 무료만 접근
        if (uid == null || uid.isBlank()) {
            boolean free = lecture.getPrice() == 0;
            return free
                    ? new LectureAccessResult(true, false, false, LectureAccessType.FREE)
                    : LectureAccessResult.none();
        }

        // 강의자 본인
        if (uid.equals(lecture.getLecturerUid())) {
            return new LectureAccessResult(true, false, false, LectureAccessType.OWNER);
        }

        // 무료
        if (lecture.getPrice() == 0) {
            return new LectureAccessResult(true, false, false, LectureAccessType.FREE);
        }

        // 단건 구매
        boolean purchased = purchaseService.hasLecturePurchase(uid, lecture.getLectureId());
        if (purchased) {
            return new LectureAccessResult(true, true, false, LectureAccessType.PURCHASE);
        }

        // 구독
        boolean subscribed = subscriptionService.hasValidAccess(uid, lecture);
        if (subscribed) {
            return new LectureAccessResult(true, false, true, LectureAccessType.SUBSCRIPTION);
        }

        return LectureAccessResult.none();
    }
}
