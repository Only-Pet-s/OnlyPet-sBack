package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.op.back.petsitter.dto.*;
import com.op.back.petsitter.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationQueryService {

    private final Firestore firestore;

    // 예약 가능한 시간 조회
    public AvailableTimeResponseDTO getAvailableTimes(String petsitterId, String date) {
        LocalDate requestDate = LocalDate.parse(date);
        LocalDate today = LocalDate.now();

        // 과거 날짜인 경우 빈 리스트
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
            throw new ReservationException("해당 날짜에는 운영이 없습니다.");
        }

        String day = toShortDay(requestDate.getDayOfWeek());

        Map<String, String> dayTime =
                (Map<String, String>) operatingTime.get(day);

        if (dayTime == null) {
            throw new ReservationException("해당 날짜에는 운영이 없습니다.");
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
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "ACCEPTED", "COMPLETED"))
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

    public List<ReadUserReservationDTO> getUserReservation(String uid) {
        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("userUid", uid)
                    .whereIn("reservationStatus", List.of("HOLD", "RESERVED", "COMPLETED", "ACCEPTED", "CANCELED", "REFUNDED"))
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
            double mannerTemp = petsitter.getDouble("mannerTemp")!= null ? petsitter.getDouble("mannerTemp") : 0;
            double rating = petsitter.getDouble("rating")!= null ? petsitter.getDouble("rating") : 0;
            result.add(new ReadUserReservationDTO(
                    doc.getId(),
                    petsitterId,
                    petsitter.getString("name"),
                    petsitter.getString("profileImageUrl"),
                    doc.getString("date"),
                    doc.getString("startTime"),
                    doc.getString("endTime"),
                    doc.getString("reservationStatus"),
                    petsitter.getString("address"),
                    petsitter.getString("phone"),
                    doc.getLong("price"),
                    readPets(doc),
                    doc.getString("requestNote"),
                    mannerTemp,
                    rating,
                    doc.getString("careType")
//                    petsitter.getDouble("mannerTemp"),
//                    petsitter.getDouble("rating")
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
                    doc.getLong("price"),
                    doc.getString("careType"),
                    doc.getString("date"),
                    doc.getString("startTime"),
                    doc.getString("endTime"),
                    readPets(doc),
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

        for (DocumentSnapshot doc : snapshots.getDocuments()) {

            total++; // 전체 횟수

            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");

            if ("CANCELED".equals(reservationStatus)) {
                canceled++; // 취소 건수
            }

            if ("REFUNDED".equals(paymentStatus)) {
                refunded++; // 환불 건수
            }

            if ("COMPLETED".equals(reservationStatus)) {
                completed++;
            }
//            if ("ACCEPTED".equals(reservationStatus) && "COMPLETED".equals(paymentStatus)) { // 예약 상태 + 결제 완료 상태 만족 시
//                LocalDate date = LocalDate.parse(doc.getString("date"));
//                LocalTime endTime = LocalTime.parse(doc.getString("endTime"));
//
//                if (LocalDateTime.of(date, endTime).isBefore(now)) { // 해당 예약 날짜가 지났으면 완료 횟수 추가
//                    completed++;
//                }
//            }
        }

        return new PetsitterReservationCountDTO(
                total,
                completed,
                refunded,
                canceled
        );
    }

    public PetsitterRevenueDTO getTotalRevenue(String petsitterId) {
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
                    .whereEqualTo("paymentStatus", "COMPLETED")
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException("수익 조회에 실패했습니다.", e);
        }

        long totalRevenue = 0;

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");
            LocalDate date = LocalDate.parse(doc.getString("date"));
            LocalTime endTime = LocalTime.parse(doc.getString("endTime"));

            if ("CANCELED".equals(reservationStatus)) {
                continue;
            }

            Long price = doc.getLong("price");
            if (price != null) {
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
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day, new ScheduleDayDTO(0, 0));
        }

        QuerySnapshot snapshots;
        try {
            snapshots = firestore.collection("reservations")
                    .whereEqualTo("petsitterId", petsitterId)
                    .whereIn("reservationStatus", List.of("ACCEPTED", "RESERVED"))
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException("예약 조회에 실패했습니다.", e);
        }

        LocalDateTime now = LocalDateTime.now();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {

            String date1 = doc.getString("date");
            String endTime1 = doc.getString("endTime");
            String reservationStatus = doc.getString("reservationStatus");
            String paymentStatus = doc.getString("paymentStatus");

            if (date1 == null || endTime1 == null) continue;

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

    // db에 들어갈 일주일 단위
    private String toShortDay(DayOfWeek dayOfWeek) {
        return dayOfWeek.name().substring(0, 3); // MONDAY -> MON
    }

    private List<PetInfoDTO> readPets(DocumentSnapshot doc) {
        Object rawPets = doc.get("pets");
        if (rawPets instanceof List<?> list) {
            List<PetInfoDTO> pets = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object nameObj = map.get("name");
                    Object typeObj = map.get("type");
                    pets.add(new PetInfoDTO(
                            nameObj != null ? nameObj.toString() : null,
                            typeObj != null ? typeObj.toString() : null
                    ));
                }
            }
            return pets;
        }

        String petName = doc.getString("petName");
        String petType = doc.getString("petType");
        if (petName != null || petType != null) {
            return List.of(new PetInfoDTO(petName, petType));
        }
        return List.of();
    }
}
