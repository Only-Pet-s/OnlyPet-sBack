package com.op.back.post.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** 조회수 관련(Redis 중복 방지 + Firestore 증가) */
@Service
@RequiredArgsConstructor
public class PostViewService {

    private final Firestore firestore;
    private final StringRedisTemplate redisTemplate;

    private static final String POSTS_COLLECTION = "posts";

    public void handleViewCount(String postId, String uid)
            throws ExecutionException, InterruptedException {

        // 비로그인 유저 제외
        if (uid == null) return;

        String key = "post:view:" + postId + ":" + uid;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        // Redis 기록 (TTL 10분)
        redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.MINUTES);

        // Firestore 증가
        increaseViewCount(postId);
    }

    private void increaseViewCount(String postId)
            throws ExecutionException, InterruptedException {

        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(postId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(postRef).get();
            Long count = Optional.ofNullable(snap.getLong("viewCount")).orElse(0L);
            tx.update(postRef, "viewCount", count + 1);
            return null;
        }).get();
    }
}
