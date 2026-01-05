package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ScheduleWeekDTO {
    private Map<DayOfWeek, ScheduleDayDTO> schedule;
}
