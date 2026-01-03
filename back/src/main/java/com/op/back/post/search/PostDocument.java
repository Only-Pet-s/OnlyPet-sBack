package com.op.back.post.search;
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
public class PostDocument {

    private String id;
    private String uid;
    private String content;
    private List<String> hashtags;
    private String mediaType;

    private int likeCount;
    private int commentCount;

    private Instant createdAt;
}