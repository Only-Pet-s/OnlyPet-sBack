package com.op.back.post.model;

import com.google.cloud.Timestamp;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
public class Post {
    private String id;
    private String uid;
    private String content;
    private String mediaUrl;
    private String mediaType;
    private List<String> hashtags;
    private Boolean commentAvailable;
    private long likeCount;
    private long commentCount;
    private long viewCount;
    private Timestamp createdAt;
}
