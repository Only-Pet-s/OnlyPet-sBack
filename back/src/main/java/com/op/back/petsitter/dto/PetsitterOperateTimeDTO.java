package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class PetsitterOperateTimeDTO {
    private String start;
    private String end;
//    private Map<String, OperatingTime> operatingTime;
//
//    @Getter
//    public static class OperatingTime {
//        private String start;
//        private String end;
//    }
}
