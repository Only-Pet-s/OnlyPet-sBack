package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.fcm.service.FcmService;
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
    private final FcmService fcmService;

    public String createReservation(String uid, ReservationRequestDTO req) {
        try {
            Map<String,Object> result = firestore.runTransaction(transaction -> {

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
                Map<String, Object> res = new HashMap<>();
                res.put("reservationId", ref.getId());
                res.put("buyerUid", uid);
                res.put("price", String.valueOf(req.getPrice()));

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
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "ACCEPTED"))
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

            // 이후 예약 취소까지
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
                (String) result.get("buyerUid"),
                reservationId
        );

        fcmService.sendReservationCancelledRefund(
                (String) result.get("buyerUid"),
                (String) result.get("buyerUid"),
                String.valueOf(result.get("refundAmount")),
                reservationId
        );

        return (CancelReservationResponseDTO) result.get("response");
    }

    public List<ReadUserReservationDTO> getUserReservation(String uid) {
        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("userUid", uid)
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "COMPLETED", "ACCEPTED","CANCELED", "REFUNDED"))
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
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "ACCEPTED"))
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

            if("ACCEPTED".equals(reservationStatus) && "COMPLETED".equals(paymentStatus)) { // 예약 상태 + 결제 완료 상태 만족 시

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

    public PetsitterRevenueDTO getTotalRevenue(String petsitterId){
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

        try{
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .whereEqualTo("paymentStatus", "COMPLETED")
                    .get().get();
        }catch(Exception e){
            throw new RuntimeException("수익 조회에 실패하였습니다.", e);
        }

        long totalRevenue = 0;

        for(DocumentSnapshot doc:snapshots.getDocuments()){
            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");
            LocalDate date = LocalDate.parse(doc.getString("date"));
            LocalTime endTime = LocalTime.parse(doc.getString("endTime"));

            if("CANCELED".equals(reservationStatus)) {
                continue;
            }

            Long price = doc.getLong("price");
            if(price != null){
                if ("RESERVED".equals(reservationStatus)
                        && "COMPLETED".equals(paymentStatus)
                        && LocalDateTime.of(date, endTime).isBefore(LocalDateTime.now())) {
                    totalRevenue += price;
                }
            }
        }

        return new PetsitterRevenueDTO(totalRevenue);
    }

    public ScheduleWeekDTO getScheduleWeek(String petsitterId) {
        DocumentSnapshot petsitter;

        try {
            petsitter = firestore.collection("users").document(petsitterId).get().get();
            if (!petsitter.getBoolean("petsitter")) {
                throw new ReservationException("펫시터 권한이 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.plusDays(6);

        Map<DayOfWeek, ScheduleDayDTO> schedule = new HashMap<>();
        for(DayOfWeek day : DayOfWeek.values()){
            schedule.put(day, new ScheduleDayDTO(0,0));
        }

        QuerySnapshot snapshots;
        try{
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .whereIn("reservationStatus", List.of("ACCEPTED", "RESERVED"))
                    .get().get();
        }catch(Exception e){
            throw new RuntimeException("예약 조회에 실패하였습니다", e);
        }

        LocalDateTime now = LocalDateTime.now();

        for(DocumentSnapshot doc:snapshots.getDocuments()){

            String date1 = doc.getString("date");
            String endTime1 = doc.getString("endTime");
            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");

            if(date1 == null || endTime1 == null) continue;

            LocalDate date = LocalDate.parse(date1);

            if (date.isBefore(startOfWeek) || date.isAfter(endOfWeek)) {
                continue;
            }

            DayOfWeek day = date.getDayOfWeek();
            ScheduleDayDTO prev = schedule.get(day);

            long total = prev.getTotal() + 1;
            long completed = prev.getCompleted();

            // 완료 조건
            if ("ACCEPTED".equals(reservationStatus)
                    && "COMPLETED".equals(paymentStatus)) {

                LocalTime endTime = LocalTime.parse(endTime1);
                if (LocalDateTime.of(date, endTime).isBefore(now)) {
                    completed++;
                }
            }

            schedule.put(day, new ScheduleDayDTO(completed, total));
        }

        return new ScheduleWeekDTO(schedule);
    }

    public void acceptReservation(String petsitterId, String reservationId){
        try{
            Map<String, String> acceptInfo=
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
                    throw new ReservationException("임시 예약 완료 상태의 요청만 수락할 수 있습니다.");
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
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void rejectReservation(String petsitterUid, String reservationId) {

        try {
            Map<String, String> rejectInfo=
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

                // 3. 상태 검증
                if (!"RESERVED".equals(doc.getString("reservationStatus"))
                        || !"COMPLETED".equals(doc.getString("paymentStatus"))) {
                    throw new ReservationException("임시 예약 완료 상태의 요청만 거절할 수 있습니다.");
                }

                // 4. 환불 계산 데이터
                String dateStr = doc.getString("date");
                String startTimeStr = doc.getString("startTime");
                Long priceLong = doc.getLong("price");

                if (dateStr == null || startTimeStr == null || priceLong == null) {
                    throw new ReservationException("예약 데이터가 올바르지 않습니다.");
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
            return;
        }

        // 3. 가상 환불 처리
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
