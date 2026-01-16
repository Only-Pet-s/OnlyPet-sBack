package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.fcm.service.FcmService;
import com.op.back.petsitter.dto.CancelReservationResponseDTO;
import com.op.back.petsitter.dto.ReservationRequestDTO;
import com.op.back.petsitter.exception.ReservationException;
import com.op.back.petsitter.util.PaymentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ReservationCommandService {

    private final Firestore firestore;
    private final FcmService fcmService;

    public String createReservation(String uid, ReservationRequestDTO req) {
        try {
            Map<String, Object> result = firestore.runTransaction(transaction -> {

                QuerySnapshot snapshots =
                        transaction.get(
                                firestore.collection("reservations")
                                        .whereEqualTo("petsitterId", req.getPetsitterId())
                                        .whereEqualTo("date", req.getDate())
                                        .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "ACCEPTED"))
                        ).get();

                LocalDate today = LocalDate.now();
                LocalDate requestDate = LocalDate.parse(req.getDate());

                if (requestDate.isBefore(today)) {
                    throw new IllegalStateException("과거 날짜에는 예약할 수 없습니다.");
                }

                LocalTime reqStart = LocalTime.parse(req.getStartTime());
                LocalTime reqEnd = LocalTime.parse(req.getEndTime());

                // 예약 시간(시간 단위)
                long hours = ChronoUnit.HOURS.between(reqStart, reqEnd);
                if (hours <= 0) {
                    throw new ReservationException("예약 시간은 최소 1시간 이상이어야 합니다.");
                }
                if(uid.equals(req.getPetsitterId())){
                    throw new ReservationException("자기 자신한테 예약할 수 없습니다.");
                }
                DocumentSnapshot petsitterInfo = firestore.collection("petsitters").document(req.getPetsitterId()).get().get();

                Long price = petsitterInfo.getLong("price");

                // 시간당 가격 x 예약 시간
                Long totalPrice = price * hours;

                DocumentSnapshot petsitter =
                        firestore.collection("petsitters")
                                .document(req.getPetsitterId())
                                .get().get();

                Map<String, Object> operatingTime =
                        (Map<String, Object>) petsitter.get("operatingTime");

                if (operatingTime == null) {
                    throw new ReservationException("해당 날짜에는 운영이 없습니다.");
                }

                String dayKey = requestDate.getDayOfWeek().name().substring(0, 3); // MON, TUE ...
                Map<String, String> dayTime =
                        (Map<String, String>) operatingTime.get(dayKey);

                if (dayTime == null) {
                    throw new ReservationException("해당 날짜에는 운영이 없습니다.");
                }

                LocalTime open = LocalTime.parse(dayTime.get("start"));
                LocalTime close = LocalTime.parse(dayTime.get("end"));

                // 운영시간 범위 검사
                if (reqStart.isBefore(open) || reqEnd.isAfter(close)) {
                    throw new ReservationException("운영시간 범위 밖에서는 예약할 수 없습니다.");
                }

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    LocalTime existStart = LocalTime.parse(doc.getString("startTime"));
                    LocalTime existEnd = LocalTime.parse(doc.getString("endTime"));

                    if (existStart.isBefore(reqEnd) && existEnd.isAfter(reqStart)) {
                        throw new ReservationException("이미 예약된 시간이 포함되어 있습니다.");
                    }
                }

                DocumentSnapshot userInfo = firestore.collection("users").document(uid).get().get();

                DocumentReference ref =
                        firestore.collection("reservations").document();

                Map<String, Object> data = new HashMap<>();
                data.put("userUid", uid);
                data.put("petsitterId", req.getPetsitterId());
                data.put("date", req.getDate());
                data.put("startTime", req.getStartTime());
                data.put("endTime", req.getEndTime());
                data.put("phone", userInfo.get("phone"));
                data.put("address", userInfo.get("address"));
                data.put("careType", req.getCareType());
                data.put("petType", req.getPetType());
                data.put("petName", req.getPetName());
                if (req.getRequestNote() != null) {
                    data.put("requestNote", req.getRequestNote());
                }
                data.put("reservationStatus", "HOLD");
                data.put("paymentStatus", "PENDING");
                data.put("price", totalPrice);
                data.put("createdAt", Timestamp.now());
                data.put(
                        "paymentExpireAt",
                        Timestamp.ofTimeSecondsAndNanos(
                                Instant.now().plus(10, ChronoUnit.MINUTES).getEpochSecond(), 0
                        )
                );

                transaction.set(ref, data);
                Map<String, Object> res = new HashMap<>();
                res.put("reservationId", ref.getId());
                res.put("buyerUid", uid);
                res.put("price", String.valueOf(totalPrice));

                return res;
            }).get();

            fcmService.sendReservationCreated(
                    (String) result.get("buyerUid"),
                    (String) result.get("price"),
                    (String) result.get("reservationId")
            );

            return (String) result.get("reservationId");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CancelReservationResponseDTO cancelReservation(
            String reservationId,
            String uid
    ) throws Exception {

        DocumentReference ref =
                firestore.collection("reservations")
                        .document(reservationId);

        Map<String, Object> result =
                firestore.runTransaction(tx -> {

                    DocumentSnapshot snap = tx.get(ref).get();

                    if (!snap.exists()) {
                        throw new ReservationException("예약이 존재하지 않습니다.");
                    }

                    if (!uid.equals(snap.getString("userUid"))) {
                        throw new ReservationException("예약 취소 권한이 없습니다.");
                    }

                    if (!("RESERVED".equals(snap.getString("reservationStatus")) ||
                            "ACCEPTED".equals(snap.getString("reservationStatus")))) {
                        throw new ReservationException("취소 가능한 예약이 아닙니다.");
                    }

                    LocalDate date = LocalDate.parse(snap.getString("date"));
                    LocalTime startTime =
                            LocalTime.parse(snap.getString("startTime"));
                    int price = snap.getLong("price").intValue();

                    // 환불 수수료
                    int fee = PaymentUtil.calculateCancelFee(date, startTime, price);
                    int refundAmount = price - fee;

                    // 환불 처리
                    paymentRefund(tx, ref, refundAmount);

                    // 이후 예약 취소 기록
                    tx.update(ref,
                            "reservationStatus", "CANCELED",
                            "paymentStatus", "REFUNDED",
                            "cancelFee", fee,
                            "refundAmount", refundAmount,
                            "canceledAt", Timestamp.now()
                    );

                    Map<String, Object> res = new HashMap<>();
                    res.put("response", new CancelReservationResponseDTO(
                            reservationId,
                            price,
                            fee,
                            refundAmount
                    ));
                    res.put("buyerUid", snap.getString("userUid"));
                    res.put("petsitterId", snap.getString("petsitterId"));
                    res.put("refundAmount", refundAmount);

                    return res;
                }).get();

        fcmService.sendReservationCancelled(
                (String) result.get("buyerUid"),
                (String) result.get("petsitterId"),
                reservationId
        );

        fcmService.sendReservationCancelledRefund(
                (String) result.get("buyerUid"),
                (String) result.get("petsitterId"),
                String.valueOf(result.get("refundAmount")),
                reservationId
        );

        return (CancelReservationResponseDTO) result.get("response");
    }

    public void acceptReservation(String petsitterId, String reservationId) {
        try {
            Map<String, String> acceptInfo =
                    firestore.runTransaction(tx -> {

                        DocumentReference ref =
                                firestore.collection("reservations").document(reservationId);

                        DocumentSnapshot doc = tx.get(ref).get();

                        if (!doc.exists()) {
                            throw new ReservationException("예약이 존재하지 않습니다.");
                        }

                        if (!petsitterId.equals(doc.getString("petsitterId"))) {
                            throw new ReservationException("예약 수락 권한이 없습니다.");
                        }

                        if (!"RESERVED".equals(doc.getString("reservationStatus"))
                                || !"COMPLETED".equals(doc.getString("paymentStatus"))) {
                            throw new ReservationException("임시 예약 완료 상태에서만 수락할 수 있습니다.");
                        }

                        tx.update(ref,
                                "reservationStatus", "ACCEPTED",
                                "acceptedAt", Timestamp.now()
                        );
                        Map<String, String> result = new HashMap<>();
                        result.put("buyerUid", doc.getString("userUid"));
                        result.put("petsitterUid", petsitterId);
                        result.put("reservationId", reservationId);

                        return result;
                    }).get();

            fcmService.sendReservationAccepted(
                    acceptInfo.get("buyerUid"),
                    acceptInfo.get("petsitterUid"),
                    acceptInfo.get("reservationId")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rejectReservation(String petsitterUid, String reservationId) {

        try {
            Map<String, String> rejectInfo =
                    firestore.runTransaction(tx -> {

                        DocumentReference ref =
                                firestore.collection("reservations").document(reservationId);

                        DocumentSnapshot doc = tx.get(ref).get();

                        // 1. 예약 존재 여부
                        if (!doc.exists()) {
                            throw new ReservationException("예약이 존재하지 않습니다.");
                        }

                        // 2. 펫시터 권한
                        if (!petsitterUid.equals(doc.getString("petsitterId"))) {
                            throw new ReservationException("예약 거절 권한이 없습니다.");
                        }

                        // 3. 상태 검사
                        if (!"RESERVED".equals(doc.getString("reservationStatus"))
                                || !"COMPLETED".equals(doc.getString("paymentStatus"))) {
                            throw new ReservationException("임시 예약 완료 상태에서만 거절할 수 있습니다.");
                        }

                        // 4. 환불 계산 전 예약 정보 확인
                        String dateStr = doc.getString("date");
                        String startTimeStr = doc.getString("startTime");
                        Long priceLong = doc.getLong("price");

                        if (dateStr == null || startTimeStr == null || priceLong == null) {
                            throw new ReservationException("예약 정보가 올바르지 않습니다.");
                        }

                        LocalDate date = LocalDate.parse(dateStr);
                        LocalTime startTime = LocalTime.parse(startTimeStr);
                        int price = priceLong.intValue();

                        // 5. 환불 수수료 계산
                        int fee = PaymentUtil.calculateCancelFee(date, startTime, price);
                        int refundAmount = Math.max(price - fee, 0);

                        // 6. 환불 처리
                        paymentRefund(tx, ref, refundAmount);

                        // 7. 예약 상태 업데이트
                        tx.update(ref,
                                "reservationStatus", "CANCELED",
                                "paymentStatus", "REFUNDED",
                                "refundAmount", refundAmount,
                                "cancelFee", fee,
                                "canceledAt", Timestamp.now()
                        );

                        Map<String, String> result = new HashMap<>();
                        result.put("buyerUid", doc.getString("userUid"));
                        result.put("petsitterUid", petsitterUid);
                        result.put("reservationId", reservationId);

                        return result;
                    }).get();

            fcmService.sendReservationRejected(
                    rejectInfo.get("buyerUid"),
                    rejectInfo.get("petsitterUid"),
                    rejectInfo.get("reservationId")
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 결제까지 완료된 예약에서 환불
    private void paymentRefund(
            Transaction tx,
            DocumentReference reservationRef,
            int refundAmount
    ) {

        DocumentSnapshot snap = null;
        try {
            snap = tx.get(reservationRef).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        // 1. 결제 상태 체크
        String paymentStatus = snap.getString("paymentStatus");
        if (!"COMPLETED".equals(paymentStatus)) {
            throw new ReservationException("환불 가능한 결제 상태가 아닙니다.");
        }

        // 2. 중복 환불 방지
        if ("REFUNDED".equals(paymentStatus)) {
            return;
        }

        // 3. 부분 환불 처리
        tx.update(reservationRef,
                "paymentStatus", "REFUNDED",
                "refundAmount", refundAmount,
                "refundAt", Timestamp.now()
        );

        // 4. 환불 알림
        fcmService.sendRefund(
                snap.getString("userUid"),
                snap.getString("petsitterId"),
                String.valueOf(snap.getLong("price")),
                reservationRef.getId()
        );
    }
}
