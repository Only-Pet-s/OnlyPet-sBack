package com.op.back.shorts.controller;

import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.service.ShortsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shorts")
@RequiredArgsConstructor
public class ShortsController {

    private final ShortsService shortsService;

    //현재 인증된 사용자 Uid 가져오기
    private String currentUid() {
        return (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // 쇼츠 생성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> create(
            @RequestPart("data") ShortsCreateRequest request,
            @RequestPart("video") MultipartFile video
    ) throws Exception {

        return ResponseEntity.ok(
                shortsService.createShorts(request, video, currentUid())
        );
    }

    // 쇼츠 단일 조회
    @GetMapping("/{shortsId}")
    public ResponseEntity<ShortsResponse> get(@PathVariable String shortsId)
            throws Exception {

        return ResponseEntity.ok(
                shortsService.getShorts(shortsId, currentUid())
        );
    }

    // 최신 쇼츠 피드
    @GetMapping("/latest")
    public ResponseEntity<List<ShortsResponse>> latest(
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {

        return ResponseEntity.ok(
                shortsService.getLatestShorts(limit, currentUid())
        );
    }

    // 좋아요
    @PostMapping("/{shortsId}/like")
    public ResponseEntity<Void> like(@PathVariable String shortsId)
            throws Exception {

        shortsService.likeShorts(shortsId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소
    @DeleteMapping("/{shortsId}/like")
    public ResponseEntity<Void> unlike(@PathVariable String shortsId)
            throws Exception {

        shortsService.unlikeShorts(shortsId, currentUid());
        return ResponseEntity.ok().build();
    }

    // 해시태그 검색
    @GetMapping("/search")
    public ResponseEntity<List<ShortsResponse>> search(
            @RequestParam String tag,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {

        return ResponseEntity.ok(
                shortsService.searchByHashtag(tag, limit, currentUid())
        );
    }
}
