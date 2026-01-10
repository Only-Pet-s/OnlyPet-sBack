package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class OperatingTimeResponseDTO {
    private String petsitterId;

    private Map<String, PetsitterOperateTimeDTO> operatingTime;
}
