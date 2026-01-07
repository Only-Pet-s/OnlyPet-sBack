package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.fcm.service.FcmService;
import com.op.back.petsitter.dto.PaymentReadyResponseDTO;
import com.op.back.petsitter.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Firestore firestore;
    private final FcmService fcmService;

    public PaymentReadyResponseDTO readyPayment(String reservationId){

        DocumentSnapshot snap = null;
        try {
            snap = firestore.collection("reservations")
                    .document(reservationId)
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!"HOLD".equals(snap.getString("reservationStatus"))) {
            throw new ReservationException("결제 가능한 예약이 아닙니다.");
        }

        String paymentId = "pay_" + UUID.randomUUID();

        snap.getReference().update(
                "paymentId", paymentId
        );

        return new PaymentReadyResponseDTO(
                paymentId,
                snap.getLong("price").intValue(),
                snap.getTimestamp("paymentExpireAt")
        );
    }

    public void paymentSuccess(
            String reservationId,
            String paymentId
    ) throws Exception {

        DocumentReference ref =
                firestore.collection("reservations")
                        .document(reservationId);

        firestore.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref).get();

            if (!snap.exists()) {
                throw new ReservationException("예약이 존재하지 않습니다.");
            }

            if (!"HOLD".equals(snap.getString("reservationStatus"))) {
                throw new ReservationException("결제 가능한 상태가 아닙니다.");
            }

            if (!paymentId.equals(snap.getString("paymentId"))) {
                throw new ReservationException("결제 정보가 일치하지 않습니다.");
            }

            // 결제 만료 체크
            if (snap.getTimestamp("paymentExpireAt").compareTo(Timestamp.now()) < 0) {
                throw new ReservationException("결제 시간이 만료되었습니다.");
            }

            tx.update(ref,
                    "reservationStatus", "RESERVED",
                    "paymentStatus", "COMPLETED",
                    "paidAt", Timestamp.now()
            );

            return null;
        }).get();

        DocumentSnapshot reservationInfo = firestore.collection("reservations").document(reservationId).get().get();

        String buyerUid = reservationInfo.getString("userUid");
        String petsitterId = reservationInfo.getString("petsitterId");
        String price = String.valueOf(reservationInfo.getLong("price"));
        String resultPaymentId = reservationInfo.getString("paymentId");

        fcmService.sendPaymentCompleted(buyerUid,petsitterId,price,resultPaymentId);
    }

    public void paymentCancel(String reservationId) throws Exception {

        DocumentReference ref =
                firestore.collection("reservations")
                        .document(reservationId);

        firestore.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref).get();

            if (!"HOLD".equals(snap.getString("reservationStatus"))) {
                throw new ReservationException("취소 가능한 상태가 아닙니다.");
            }

            tx.update(ref,
                    "reservationStatus", "CANCELED",
                    "paymentStatus", "CANCELED"
            );

            return null;
        }).get();
    }
}
