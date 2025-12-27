package com.op.back.myPage.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.myPage.dto.MyPageDTO;
import com.op.back.myPage.dto.MyPagePostDTO;
import com.op.back.myPage.dto.PageVisibleDTO;
import com.op.back.myPage.dto.PageVisibleRequestDTO;
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

    // 프로필 조회 -> pageVisible이 PUBLIC인 경우만 조회를 할 수 있다.
    @GetMapping("/{uid}")
    public ResponseEntity<MyPageDTO> getProfile(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader
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

    // 공개 범위 변경, 로그인 uid == uid
    @PatchMapping("/pageVisible")
    public ResponseEntity<PageVisibleDTO> updatePageVisible(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PageVisibleRequestDTO request
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));

        myPageService.updatePageVisible(uid, request.getPageVisible());

        return ResponseEntity.ok(
                new PageVisibleDTO(request.getPageVisible())
        );
    }
}