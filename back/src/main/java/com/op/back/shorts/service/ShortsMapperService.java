package com.op.back.shorts.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.model.Shorts;
import com.op.back.shorts.search.ShortsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Shorts <-> Response 변환 및 공통 유틸 */
@Service
@RequiredArgsConstructor
public class ShortsMapperService {

    private final Firestore firestore;

    public Shorts toShorts(DocumentSnapshot doc) {
        return Shorts.builder()
                .id(doc.getId())
                .uid(doc.getString("uid"))
                .mediaUrl(doc.getString("mediaUrl"))
                .thumbnailUrl(doc.getString("thumbnailUrl"))
                .description(doc.getString("description"))
                .hashtags((List<String>) doc.get("hashtags"))
                .likeCount(Optional.ofNullable(doc.getLong("likeCount")).orElse(0L))
                .commentCount(Optional.ofNullable(doc.getLong("commentCount")).orElse(0L))
                .viewCount(Optional.ofNullable(doc.getLong("viewCount")).orElse(0L))
                .createdAt(doc.getTimestamp("createdAt"))
                .build();
    }

    public ShortsResponse toResponse(Shorts s, boolean liked, boolean bookmarked, String currentUid) {
        Instant createdAt = s.getCreatedAt() != null
                ? Instant.ofEpochSecond(
                s.getCreatedAt().getSeconds(),
                s.getCreatedAt().getNanos()
        )
                : null;

        return ShortsResponse.builder()
                .id(s.getId())
                .uid(s.getUid())
                .nickname(getNickname(s.getUid()))
                .mediaUrl(s.getMediaUrl())
                .thumbnailUrl(s.getThumbnailUrl())
                .description(s.getDescription())
                .hashtags(s.getHashtags())
                .likeCount(s.getLikeCount())
                .commentCount(s.getCommentCount())
                .viewCount(s.getViewCount())
                .liked(liked)
                .bookmarked(bookmarked)
                .mine(s.getUid().equals(currentUid))
                .createdAt(createdAt)
                .build();
    }

    /**
     * Elasticsearch 검색 결과 → ShortsResponse 변환 (Firestore 재조회 X)
     */
    public ShortsResponse toSearchResponse(ShortsDocument doc) {
        return ShortsResponse.builder()
                .id(doc.getId())
                .uid(doc.getUid())
                .nickname(getNickname(doc.getUid()))
                .mediaUrl(null)
                .thumbnailUrl(null)
                .description(doc.getDescription())
                .hashtags(doc.getHashtags())
                .likeCount(0L)
                .commentCount(0L)
                .viewCount(doc.getViewCount())
                .liked(false)
                .bookmarked(false)
                .mine(false)
                .createdAt(
                        doc.getCreatedAt() != null
                                ? Instant.parse(doc.getCreatedAt())
                                : null
                )
                .build();
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

    public Instant toInstant(Timestamp ts) {
        if (ts == null) return null;
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
}
