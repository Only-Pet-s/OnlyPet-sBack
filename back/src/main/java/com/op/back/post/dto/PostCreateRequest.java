package com.op.back.post.dto;

import java.util.List;

public class PostCreateRequest {
    //게시글 텍스트 내용
    private String content;
    // img or vid
    private String mediaType;
    //해시태그 목록
    private List<String> hashtags;
    //공개범위
    private Boolean commentAvailable;

    public Boolean getCommentAvailable() {
        return commentAvailable;
    }

    public void setCommentAvailable(Boolean commentAvailable) {
        this.commentAvailable = commentAvailable;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
