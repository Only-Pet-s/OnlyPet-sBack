package com.op.back.petsitter.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class OperatingTimeUpdateRequestDTO {

    // key: MON, TUE, WED ...
    private Map<String, PetsitterOperateTimeDTO> operatingTime;
}
