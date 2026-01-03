package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PetsitterReservationCountDTO {
    private long totalCount;
    private long completedCount;
    private long refundedCount;
    private long canceledCount;
}
