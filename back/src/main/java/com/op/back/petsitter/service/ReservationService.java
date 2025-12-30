package com.op.back.petsitter.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.op.back.petsitter.dto.ReservationRequestDTO;
import com.op.back.petsitter.exception.ReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                LocalTime reqEnd   = LocalTime.parse(req.getEndTime());

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    LocalTime existStart = LocalTime.parse(doc.getString("startTime"));
                    LocalTime existEnd   = LocalTime.parse(doc.getString("endTime"));

                    if (existStart.isBefore(reqEnd) && existEnd.isAfter(reqStart)) {
                        throw new ReservationException("이미 예약된 시간이 포함되어 있습니다.");
                    }
                }

                DocumentReference ref =
                        firestore.collection("reservations").document();

                Map<String, Object> data = new HashMap<>();
                data.put("userUid", uid);
                data.put("petsitterId", req.getPetsitterId());
                data.put("date", req.getDate());
                data.put("startTime", req.getStartTime());
                data.put("endTime", req.getEndTime());
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
}
