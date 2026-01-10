package com.op.back.petsitter.scheduler;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private final Firestore firestore;

    @Scheduled(fixedDelay = 60000) // 1분마다
    public void expireHoldReservations() throws Exception {

        QuerySnapshot expired =
                firestore.collection("reservations")
                        .whereEqualTo("reservationStatus", "HOLD")
                        .whereLessThan("paymentExpireAt", Timestamp.now())
                        .get().get();

        for (DocumentSnapshot doc : expired.getDocuments()) {
            doc.getReference().update(
                    "reservationStatus", "CANCELED",
                    "paymentStatus", "CANCELED"
            );
        }
    }
}
