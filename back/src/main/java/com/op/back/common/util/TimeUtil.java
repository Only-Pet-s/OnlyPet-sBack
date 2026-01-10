package com.op.back.common.util;
import com.google.cloud.Timestamp;
import java.time.Instant;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static Instant toInstant(Timestamp ts) {
        if (ts == null) return null;
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
}