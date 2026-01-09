package com.op.back.access;

public record LectureAccessResult(
        boolean accessible,
        boolean purchased,
        boolean subscribed,
        LectureAccessType type
) {
    public static LectureAccessResult none() {
        return new LectureAccessResult(false, false, false, LectureAccessType.NONE);
    }
}
