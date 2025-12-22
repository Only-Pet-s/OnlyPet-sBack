package com.op.back.myPage.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.myPage.dto.MyPageDTO;
import com.op.back.myPage.dto.MyPagePostDTO;
import com.op.back.myPage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/myPage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final JwtUtil jwtUtil;

    // 프로필 조회 (내 페이지 + 타인 페이지 공용)
    @GetMapping("/{uid}")
    public ResponseEntity<MyPageDTO> getProfile(
            @PathVariable String uid,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) throws Exception {

        String loginUid = jwtUtil.getUidOrNull(authHeader);

        return ResponseEntity.ok(
                myPageService.getProfile(uid, loginUid)
        );
    }

    // 게시물 썸네일 목록 (공개)
    @GetMapping("/{uid}/posts")
    public ResponseEntity<List<MyPagePostDTO>> getMyPosts(
            @PathVariable String uid
    ) throws Exception {

        return ResponseEntity.ok(
                myPageService.getMyPosts(uid)
        );
    }
}

