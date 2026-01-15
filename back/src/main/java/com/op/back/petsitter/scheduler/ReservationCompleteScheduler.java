package com.op.back.petsitter.scheduler;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ExecutionException;

@Service
@AllArgsConstructor
public class ReservationCompleteScheduler {

    private final Firestore firestore;

    @Scheduled(cron = "0 0 * * * *") // 매 정각
    public void completeExpiredReservations() {
        QuerySnapshot snapshot = null;
        try {
            snapshot = firestore.collection("reservations")
                    .whereEqualTo("paymentStatus", "COMPLETED")
                    .whereEqualTo("reservationStatus", "ACCEPTED")
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            LocalDate date = LocalDate.parse(doc.getString("date"));
            LocalTime end = LocalTime.parse(doc.getString("endTime"));

            LocalDateTime endDateTime = LocalDateTime.of(date, end);

            if (endDateTime.isBefore(LocalDateTime.now())) {
                doc.getReference().update("reservationStatus", "COMPLETED");
            }
        }
    }
}
