package com.op.back.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "RT:";

    // refresh token 저장
    public void saveRefreshToken(String uid, String refreshToekn, long duration){
        redisTemplate.opsForValue()
                .set(PREFIX + uid, refreshToekn, duration, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(String uid){
        return redisTemplate.opsForValue().get(PREFIX + uid);
    }

    public void deleteRefreshToken(String uid){
        redisTemplate.delete(PREFIX + uid);
    }
}
