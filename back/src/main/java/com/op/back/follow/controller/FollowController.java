package com.op.back.follow.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.follow.dto.FollowUserDTO;
import com.op.back.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final JwtUtil jwtUtil;

    // 팔로우
    @PostMapping("/{targetUid}")
    public ResponseEntity<Void> follow(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String targetUid
    ) {
        String myUid = jwtUtil.getUid(authHeader.substring(7));
        followService.follow(myUid, targetUid);
        return ResponseEntity.ok().build();
    }

    // 언팔로우
    @DeleteMapping("/{targetUid}")
    public ResponseEntity<Void> unfollow(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String targetUid
    ) {
        String myUid = jwtUtil.getUid(authHeader.substring(7));
        followService.unfollow(myUid, targetUid);
        return ResponseEntity.ok().build();
    }

    // 팔로워 목록
    @GetMapping("/followers/{uid}")
    public ResponseEntity<List<FollowUserDTO>> followers(@PathVariable String uid) {
        return ResponseEntity.ok(followService.getFollowers(uid));
    }

    // 팔로잉 목록
    @GetMapping("/following/{uid}")
    public ResponseEntity<List<FollowUserDTO>> following(@PathVariable String uid) {
        return ResponseEntity.ok(followService.getFollowing(uid));
    }
}

