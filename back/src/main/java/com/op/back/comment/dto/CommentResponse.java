package com.op.back.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class CommentResponse {

    private String id;
    private String parentId;

    private Author author;
    private String content;
    private Stats stats;
    private Timestamps timestamps;

    private List<CommentResponse> children;

    @Getter
    @Builder
    public static class Author {
        private String uid;
        private String nickname;
    }

    @Getter
    @Builder
    public static class Stats {
        private long likeCount;
        private boolean liked;
    }

    @Getter
    @Builder
    public static class Timestamps {
        private Instant createdAt;
        private Instant updatedAt;
        private boolean edited;
    }
}
