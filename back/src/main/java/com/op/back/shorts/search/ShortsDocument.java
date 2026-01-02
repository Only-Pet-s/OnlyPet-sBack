package com.op.back.shorts.search;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortsDocument {

    private String id;
    private String uid;

    private String description;
    private List<String> hashtags;

    private int viewCount;
    private Instant createdAt;
}