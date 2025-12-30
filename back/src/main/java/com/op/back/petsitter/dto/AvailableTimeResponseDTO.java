package com.op.back.petsitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AvailableTimeResponseDTO {
    private String petsitterId;
    private String date;
    private List<String> times;
}
