package com.op.back.petsitter.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class PaymentUtil {
    public static int calculateCancelFee(
            LocalDate date,
            LocalTime startTime,
            int price
    ) {
        LocalDateTime startDateTime =
                LocalDateTime.of(date, startTime);

        long minutes =
                Duration.between(LocalDateTime.now(), startDateTime)
                        .toMinutes();

        if (minutes >= 2880) return 0;   // 48시간 이상
        if (minutes >= 1440) return price * 10 / 100; // 24
        if (minutes >= 720)  return price * 20 / 100; // 12
        if (minutes >= 120)  return price * 30 / 100; // 2
        return price * 40 / 100;
    }
}
