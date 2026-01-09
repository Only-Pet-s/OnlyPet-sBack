package com.op.back.shorts.service;

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
public class ShortsViewService {

    private final Firestore firestore;
    private final StringRedisTemplate redisTemplate;

    private static final String SHORTS = "shorts";

    public void handleViewCount(String shortsId, String uid)
            throws ExecutionException, InterruptedException {

        // 비로그인 유저는 제외
        if (uid == null) return;

        String key = "shorts:view:" + shortsId + ":" + uid;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        // Redis 기록 (TTL 10분)
        redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.MINUTES);

        // Firestore 조회수 증가
        increaseViewCount(shortsId);
    }

    private void increaseViewCount(String shortsId)
            throws ExecutionException, InterruptedException {
        DocumentReference sRef = firestore.collection(SHORTS).document(shortsId);

        firestore.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(sRef).get();
            Long count = Optional.ofNullable(snap.getLong("viewCount")).orElse(0L);
            tx.update(sRef, "viewCount", count + 1);
            return null;
        }).get();
    }
}
