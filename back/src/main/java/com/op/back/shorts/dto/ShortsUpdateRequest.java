package com.op.back.shorts.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShortsUpdateRequest {

    private String description;
    private List<String> hashtags;

    private Boolean commentAvailable;
}