package com.op.back.post.controller;

import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    //게시글 작성
    @PostMapping
    public ResponseEntity<String> createPost(
            @RequestPart(value="file", required = false) MultipartFile file,
            @RequestPart("data") PostCreateRequest request
    ) throws IOException, ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        String postId = postService.createPost(request, file, uid);
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    //최신 게시글 목록 조회
    @GetMapping
    public ResponseEntity<List<PostResponse>> getLatestPosts(
            @RequestParam(defaultValue = "20") int limit
    ) throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        List<PostResponse> posts = postService.getLatestPosts(limit, uid);
        return ResponseEntity.ok(posts);
    }

    //게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        PostResponse post = postService.getPost(postId, uid);
        return ResponseEntity.ok(post);
    }

    //게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        postService.deletePost(postId, uid);
        return ResponseEntity.noContent().build();
    }

    //좋아요 추가
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        postService.likePost(postId, uid);
        return ResponseEntity.ok().build();
    }

    //좋아요 취소
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        postService.unlikePost(postId, uid);
        return ResponseEntity.ok().build();
    }

    //북마크 추가
    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<Void> bookmarkPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        postService.bookmarkPost(postId, uid);
        return ResponseEntity.ok().build();
    }

    //북마크 취소
    @DeleteMapping("/{postId}/bookmark")
    public ResponseEntity<Void> unbookmarkPost(@PathVariable String postId)
            throws ExecutionException, InterruptedException {
        String uid = getCurrentUid();
        postService.unbookmarkPost(postId, uid);
        return ResponseEntity.ok().build();
    }

    //해시태그 검색
    @GetMapping("/search")
    public ResponseEntity<List<PostResponse>> searchByHashtag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "20") int limit
    ) throws ExecutionException, InterruptedException {

        String uid = getCurrentUid();
        List<PostResponse> posts = postService.searchByHashtag(tag, limit, uid);
        return ResponseEntity.ok(posts);
    }


    // **유틸** //
    private String getCurrentUid() {
        // TODO: 실제 구현
        //  - JWT 필터에서 SecurityContext 에 넣어두었다면:
        //    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //    return (String) auth.getPrincipal();
        //
        // 지금은 테스트용으로 하드코딩 가능.
        return "TEST_UID";
    }
}
