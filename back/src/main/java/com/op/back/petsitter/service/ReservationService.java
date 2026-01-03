package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.petsitter.dto.*;
import com.op.back.petsitter.exception.ReservationException;
import com.op.back.petsitter.util.PaymentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final Firestore firestore;

    public String createReservation(String uid, ReservationRequestDTO req) {
        try {
            return firestore.runTransaction(transaction -> {

                QuerySnapshot snapshots =
                        transaction.get(
                                firestore.collection("reservations")
                                        .whereEqualTo("petsitterId", req.getPetsitterId())
                                        .whereEqualTo("date", req.getDate())
                                        .whereIn("reservationStatus", List.of("HOLD", "RESERVED"))
                        ).get();

                LocalDate today = LocalDate.now();
                LocalDate requestDate = LocalDate.parse(req.getDate());

                if (requestDate.isBefore(today)) {
                    throw new IllegalStateException("과거 날짜에는 예약할 수 없습니다.");
                }

                LocalTime reqStart = LocalTime.parse(req.getStartTime());
                LocalTime reqEnd = LocalTime.parse(req.getEndTime());

                DocumentSnapshot petsitter =
                        firestore.collection("petsitters")
                                .document(req.getPetsitterId())
                                .get().get();

                Map<String, Object> operatingTime =
                        (Map<String, Object>) petsitter.get("operatingTime");

                if (operatingTime == null) {
                    throw new ReservationException("해당 날짜에는 운영하지 않습니다.");
                }

                String dayKey = requestDate.getDayOfWeek().name().substring(0, 3); // MON, TUE ...
                Map<String, String> dayTime =
                        (Map<String, String>) operatingTime.get(dayKey);

                if (dayTime == null) {
                    throw new ReservationException("해당 날짜에는 운영하지 않습니다.");
                }

                LocalTime open = LocalTime.parse(dayTime.get("start"));
                LocalTime close = LocalTime.parse(dayTime.get("end"));

                // 운영시간 범위 검증
                if (reqStart.isBefore(open) || reqEnd.isAfter(close)) {
                    throw new ReservationException("운영시간 외에는 예약할 수 없습니다.");
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
                data.put("price", req.getPrice());
                data.put("createdAt", Timestamp.now());
                data.put(
                        "paymentExpireAt",
                        Timestamp.ofTimeSecondsAndNanos(
                                Instant.now().plus(10, ChronoUnit.MINUTES).getEpochSecond(), 0
                        )
                );

                transaction.set(ref, data);
                return ref.getId();
            }).get();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 예약 가능 시간 조회
    public AvailableTimeResponseDTO getAvailableTimes(String petsitterId, String date) {
        LocalDate requestDate = LocalDate.parse(date);
        LocalDate today = LocalDate.now();

        // 과거 날짜일 경우 빈 칸
        if (requestDate.isBefore(today)) {
            return new AvailableTimeResponseDTO(
                    petsitterId, date, List.of()
            );
        }

        // 1. 펫시터 운영시간 조회
        DocumentSnapshot petsitter;
        try {
            petsitter = firestore.collection("petsitters")
                    .document(petsitterId)
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException("펫시터 조회 실패", e);
        }

        Map<String, Object> operatingTime =
                (Map<String, Object>) petsitter.get("operatingTime");

        if (operatingTime == null) {
            throw new ReservationException("해당 날짜에는 운영하지 않습니다.");
        }

        String day = toShortDay(requestDate.getDayOfWeek());

        Map<String, String> dayTime =
                (Map<String, String>) operatingTime.get(day);

        if (dayTime == null) {
            throw new ReservationException("해당 날짜에는 운영하지 않습니다.");
        }

        LocalTime start = LocalTime.parse(dayTime.get("start"));
        LocalTime end = LocalTime.parse(dayTime.get("end"));

        // 2. 운영시간 전체 리스트 생성
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = start; t.isBefore(end); t = t.plusHours(1)) {
            slots.add(t);
        }

        // 3. 오늘이면 현재 시간 이전 슬롯 제거
        if (requestDate.equals(today)) {
            LocalTime now = LocalTime.now();
            slots.removeIf(t -> !t.isAfter(now));
        }

        // 4. 기존 예약 조회
        QuerySnapshot reservations;
        try {
            reservations = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .whereEqualTo("date", date)
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED"))
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5. 예약 시간 제거
        for (DocumentSnapshot doc : reservations.getDocuments()) {
            LocalTime rs = LocalTime.parse(doc.getString("startTime"));
            LocalTime re = LocalTime.parse(doc.getString("endTime"));

            slots.removeIf(slot ->
                    slot.isBefore(re) && slot.plusHours(1).isAfter(rs)
            );
        }

        return new AvailableTimeResponseDTO(
                petsitterId,
                date,
                slots.stream()
                        .map(LocalTime::toString)
                        .toList()
        );
    }

    public CancelReservationResponseDTO cancelReservation(
            String reservationId,
            String uid
    ) throws Exception {

        DocumentReference ref =
                firestore.collection("reservations")
                        .document(reservationId);

        return firestore.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref).get();

            if (!snap.exists()) {
                throw new ReservationException("예약이 존재하지 않습니다.");
            }

            if (!uid.equals(snap.getString("userUid"))) {
                throw new ReservationException("예약 취소 권한이 없습니다.");
            }

            if (!"RESERVED".equals(snap.getString("reservationStatus"))) {
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

            // 이후 예약 취소까지
            tx.update(ref,
                    "reservationStatus", "CANCELED",
                    "paymentStatus", "REFUNDED",
                    "cancelFee", fee,
                    "refundAmount", refundAmount,
                    "canceledAt", Timestamp.now()
            );

            return new CancelReservationResponseDTO(
                    reservationId,
                    price,
                    fee,
                    refundAmount
            );
        }).get();
    }

    public List<ReadUserReservationDTO> getUserReservation(String uid) {
        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("userUid", uid)
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "COMPLETED", "CANCELED", "REFUNDED"))
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ReadUserReservationDTO> result = new ArrayList<>();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            String petsitterId = doc.getString("petsitterId");

            DocumentSnapshot petsitter;
            try {
                petsitter = firestore.collection("petsitters")
                        .document(petsitterId)
                        .get().get();
            } catch (Exception e) {
                continue;
            }

            result.add(new ReadUserReservationDTO(
                    doc.getId(),
                    petsitterId,
                    petsitter.getString("name"),
                    petsitter.getString("profileImageUrl"),
                    doc.getString("date"),
                    doc.getString("startTime"),
                    doc.getString("endTime"),
                    doc.getString("reservationStatus")
            ));
        }

        return result;
    }

    public List<ReadPetsitterReservedDTO> getPetsitterReserved(String petsitterId) {
        DocumentSnapshot petsitter;
        try {
            petsitter = firestore.collection("users").document(petsitterId).get().get();
            if (!petsitter.getBoolean("petsitter")) {
                throw new ReservationException("펫시터 권한이 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED"))
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ReadPetsitterReservedDTO> result = new ArrayList<>();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            String userUid = doc.getString("userUid");

            DocumentSnapshot user;
            try {
                user = firestore.collection("users")
                        .document(userUid)
                        .get().get();
            } catch (Exception e) {
                continue;
            }

            result.add(new ReadPetsitterReservedDTO(
                    doc.getId(),
                    userUid,
                    user.getString("name"),
                    user.getString("profileImageUrl"),
                    user.getString("phone"),
                    user.getString("address"),
                    doc.getString("careType"),
                    doc.getString("date"),
                    doc.getString("startTime"),
                    doc.getString("endTime"),
                    doc.getString("petType"),
                    doc.getString("petName"),
                    doc.getString("requestNote"),
                    doc.getString("reservationStatus")
            ));
        }

        return result;
    }

    public PetsitterReservationCountDTO getReservationCount(String petsitterId) {
        long total = 0;
        long completed = 0;
        long refunded = 0;
        long canceled = 0;
        LocalDateTime now = LocalDateTime.now();

        DocumentSnapshot petsitter;
        try {
            petsitter = firestore.collection("users").document(petsitterId).get().get();
            if (!petsitter.getBoolean("petsitter")) {
                throw new ReservationException("해당 펫시터가 존재하지 않습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for(DocumentSnapshot doc:snapshots.getDocuments()) {

            total++; // 전체 횟수

            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");

            if("CANCELED".equals(reservationStatus)) {
                canceled++; // 취소 건수
            }

            if("REFUNDED".equals(paymentStatus)) {
                refunded++; // 환불 건수
            }

            if("RESERVED".equals(reservationStatus) && "COMPLETED".equals(paymentStatus)) { // 예약 상태 + 결제 완료 상태 만족 시

                LocalDate date = LocalDate.parse(doc.getString("date"));
                LocalTime endTime = LocalTime.parse(doc.getString("endTime"));

                if(LocalDateTime.of(date, endTime).isBefore(now)){ // 해당 예약 날짜와 시간이 지나면 완료 횟수 추가
                    completed++;
                }
            }
        }

        return new PetsitterReservationCountDTO(
                total,
                completed,
                refunded,
                canceled
        );
    }

    // db에 들어갈 요일 형식
    private String toShortDay(DayOfWeek dayOfWeek) {
        return dayOfWeek.name().substring(0, 3); // MONDAY -> MON
    }

    // 결제까지 완료된 예약에 대한 환불
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

        // 1. 결제 상태 검증
        String paymentStatus = snap.getString("paymentStatus");
        if (!"COMPLETED".equals(paymentStatus)) {
            throw new ReservationException("환불 가능한 결제 상태가 아닙니다.");
        }

        // 2. 중복 환불 방지
        if ("REFUNDED".equals(paymentStatus)) {
            return; // 이미 환불됨 -> idempotent
        }

        // 3. 가상 환불 처리
        tx.update(reservationRef,
                "paymentStatus", "REFUNDED",
                "refundAmount", refundAmount,
                "refundAt", Timestamp.now()
        );
    }
}
