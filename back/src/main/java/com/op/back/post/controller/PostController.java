package com.op.back.post.controller;

import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    //현재 인증된 사용자 Uid 가져오기
    private String currentUid() {
        return (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }


    //게시글 작성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @RequestPart("data") PostCreateRequest request,
            @RequestPart("media") MultipartFile media
    ) throws Exception {

        return ResponseEntity.ok(
                postService.createPost(request, media, currentUid())
        );
    }

    //최신 게시글 목록 조회
    @GetMapping
    public ResponseEntity<List<PostResponse>> getLatestPosts(
            @RequestParam(defaultValue = "20") int limit
    ) throws ExecutionException, InterruptedException {

        return ResponseEntity.ok(
                postService.getLatestPosts(limit, currentUid())
        );
    }

    //게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {

        return ResponseEntity.ok(
                postService.getPost(postId, currentUid())
        );
    }

    //게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId)
            throws Exception {

        postService.deletePost(postId, currentUid());
        return ResponseEntity.ok().build();
    }

    //좋아요 추가
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> like(@PathVariable String postId)
            throws Exception {

        postService.likePost(postId, currentUid());
        return ResponseEntity.ok().build();
    }

    //좋아요 취소
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlike(@PathVariable String postId)
            throws Exception {

        postService.unlikePost(postId, currentUid());
        return ResponseEntity.ok().build();
    }

    //북마크 추가
    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<Void> bookmarkPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {

        postService.bookmarkPost(postId, currentUid());
        return ResponseEntity.ok().build();
    }

    //북마크 취소
    @DeleteMapping("/{postId}/bookmark")
    public ResponseEntity<Void> unbookmarkPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {

        postService.unbookmarkPost(postId, currentUid());
        return ResponseEntity.ok().build();
    }

    //검색 [엘라스틱]
    @GetMapping("/search")
    public List<PostResponse> search(@RequestParam String q) {
        return postService.search(q);
    }
    
    //내가 누른 좋아요 포스트
    @GetMapping("/{uid}/likes")
    public List<PostResponse> getLikedPosts(@PathVariable String uid)
            throws Exception {
        return postService.getLikedPosts(uid);
    }

    //내가 누른 북마크 포스트
    @GetMapping("/{uid}/bookmarks")
    public List<PostResponse> getBookmarkedPosts(@PathVariable String uid)
            throws Exception {
        return postService.getBookmarkedPosts(uid);
    }
}
