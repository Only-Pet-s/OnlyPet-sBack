package com.op.back.post.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.model.Post;
import com.op.back.post.search.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Post <-> Response 변환 및 공통 유틸 */
@Service
@RequiredArgsConstructor
public class PostMapperService {

    private final Firestore firestore;

    public Post toPost(DocumentSnapshot doc) {
        return Post.builder()
                .id(doc.getId())
                .uid(doc.getString("uid"))
                .content(doc.getString("content"))
                .mediaUrl(doc.getString("mediaUrl"))
                .mediaType(doc.getString("mediaType"))
                .hashtags((List<String>) doc.get("hashtags"))
                .commentAvailable(doc.getBoolean("commentAvailable"))
                .likeCount(Optional.ofNullable(doc.getLong("likeCount")).orElse(0L))
                .commentCount(Optional.ofNullable(doc.getLong("commentCount")).orElse(0L))
                .viewCount(Optional.ofNullable(doc.getLong("viewCount")).orElse(0L))
                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    public PostResponse toListResponse(Post post, boolean liked, boolean bookmarked, String currentUid) {
        return PostResponse.builder()
                .id(post.getId())
                .uid(post.getUid())
                .nickname(getNickname(post.getUid()))
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .commentAvailable(post.getCommentAvailable())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .mine(post.getUid().equals(currentUid))
                .createdAt(toInstant(post.getCreatedAt()))
                .build();
    }

    public PostResponse toDetailResponse(Post post, boolean liked, boolean bookmarked, String currentUid) {
        return PostResponse.builder()
                .id(post.getId())
                .uid(post.getUid())
                .nickname(getNickname(post.getUid()))
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .mediaType(post.getMediaType())
                .hashtags(post.getHashtags())
                .commentAvailable(post.getCommentAvailable())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .mine(post.getUid().equals(currentUid))
                .createdAt(toInstant(post.getCreatedAt()))
                .build();
    }

    /**
     * Elasticsearch 검색 결과 → PostResponse 변환
     * (Firestore 조회 안 함, 검색 전용)
     */
    public PostResponse toSearchResponse(PostDocument doc) {
        return PostResponse.builder()
                .id(doc.getId())
                .uid(doc.getUid())
                .nickname(getNickname(doc.getUid()))
                .content(doc.getContent())
                .mediaType(doc.getMediaType())
                .hashtags(doc.getHashtags())
                .likeCount(doc.getLikeCount())
                .commentCount(doc.getCommentCount())
                .viewCount(0L)
                .liked(false)
                .bookmarked(false)
                .mine(false)
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public Instant toInstant(Timestamp ts) {
        if (ts == null) return null;
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    // 닉네임 조회
    public String getNickname(String uid) {
        if (uid == null) return null;
        try {
            DocumentSnapshot userDoc = firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .get();

            if (!userDoc.exists()) return null;
            return userDoc.getString("nickname");
        } catch (Exception e) {
            return null;
        }
    }
}
