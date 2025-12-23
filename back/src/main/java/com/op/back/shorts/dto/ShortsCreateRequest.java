package com.op.back.shorts.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShortsCreateRequest {
    private String description;
    private List<String> hashtags;
}
