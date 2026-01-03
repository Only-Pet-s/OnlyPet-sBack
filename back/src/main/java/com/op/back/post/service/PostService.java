package com.op.back.post.service;

import com.op.back.post.dto.PostCreateRequest;
import com.op.back.post.dto.PostResponse;
import com.op.back.post.dto.PostUpdateRequest;
import com.op.back.post.search.PostSearchRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Facade service.
 *
 * 기존 PostService -> (생성/조회/미디어/리액션/검색/조회수) 역할별 서비스로 분리함
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostReactionService postReactionService;
    private final PostSearchService postSearchService;
    private final PostSearchRepository postSearchRepository;

    // 게시글 생성
    public PostResponse createPost(PostCreateRequest request, List<MultipartFile> mediaFiles, String uid)
            throws IOException, ExecutionException, InterruptedException {
        return postCommandService.createPost(request, mediaFiles, uid);
    }

    // 최신 게시글 목록 조회
    public List<PostResponse> getLatestPosts(int limit, String currentUid)
            throws ExecutionException, InterruptedException {
        return postQueryService.getLatestPosts(limit, currentUid);
    }

    // 단일 게시글 조회
    public PostResponse getPost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {
        return postQueryService.getPost(postId, currentUid);
    }

    // 게시글 수정 (미디어: 추가/삭제/순서변경)
    public PostResponse updatePost(String postId, PostUpdateRequest request,
                                  List<MultipartFile> newMediaFiles, String currentUid) throws Exception {
        return postCommandService.updatePost(postId, request, newMediaFiles, currentUid);
    }

    // 게시글 삭제(only owner)
    public void deletePost(String postId, String currentUid)
            throws ExecutionException, InterruptedException {
        postCommandService.deletePost(postId, currentUid);
    }

    // 좋아요 추가
    public void likePost(String postId, String uid)
            throws ExecutionException, InterruptedException {
        postReactionService.likePost(postId, uid);
    }

    // 좋아요 취소
    public void unlikePost(String postId, String uid)
            throws ExecutionException, InterruptedException {
        postReactionService.unlikePost(postId, uid);
    }

    // 북마크 추가
    public void bookmarkPost(String postId, String uid)
            throws ExecutionException, InterruptedException {
        postReactionService.bookmarkPost(postId, uid);
    }

    // 북마크 제거
    public void unbookmarkPost(String postId, String uid)
            throws ExecutionException, InterruptedException {
        postReactionService.unbookmarkPost(postId, uid);
    }

    // 해시태그 검색 (Firestore)
    public List<PostResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {
        return postQueryService.searchByHashtag(tag, limit, currentUid);
    }

    /*
        엘라스틱 서치 기반 검색
    */
    // public List<PostResponse> search(String q) {
    //     return postSearchService.search(q);
    // }

    public List<PostResponse> search(String keyword, int size, String currentUid)
        throws ExecutionException, InterruptedException {
        List<String> ids =
                postSearchRepository.searchPostIds(keyword, size);

        if (ids.isEmpty()) 
            return List.of();

        List<PostResponse> result = new ArrayList<>();
        for (String postId : ids) {
            PostResponse response =
                    postQueryService.getPost(postId, currentUid);
            result.add(response);
        }
        return result;
    }
    
}