package com.op.back.shorts.controller;

import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.service.ShortsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shorts")
@RequiredArgsConstructor
public class ShortsController {

    private final ShortsService service;

    private String getUid() { return "TEST_UID"; }

    @PostMapping
    public String create(
            @RequestPart("data") ShortsCreateRequest request,
            @RequestPart("video") MultipartFile video
    ) throws Exception {
        return service.createShorts(request, video, getUid());
    }

    @GetMapping("/{shortsId}")
    public ShortsResponse get(@PathVariable String shortsId) throws Exception {
        return service.getShorts(shortsId, getUid());
    }

    @GetMapping("/latest")
    public List<ShortsResponse> latest(@RequestParam(defaultValue = "20") int limit) throws Exception {
        return service.getLatestShorts(limit, getUid());
    }

    @PostMapping("/{shortsId}/like")
    public void like(@PathVariable String shortsId) throws Exception {
        service.likeShorts(shortsId, getUid());
    }

    @DeleteMapping("/{shortsId}/like")
    public void unlike(@PathVariable String shortsId) throws Exception {
        service.unlikeShorts(shortsId, getUid());
    }

    @PostMapping("/{shortsId}/view")
    public void view(@PathVariable String shortsId) throws Exception {
        service.increaseViewCount(shortsId);
    }

    @GetMapping("/search")
    public List<ShortsResponse> search(
            @RequestParam String tag,
            @RequestParam(defaultValue = "20") int limit
    ) throws Exception {
        return service.searchByHashtag(tag, limit, getUid());
    }
}
