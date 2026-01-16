package com.op.back.shorts.service;

import com.op.back.shorts.dto.ShortsCreateRequest;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.dto.ShortsUpdateRequest;
import com.op.back.shorts.search.ShortsSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Facade service
 * 기존 ShortsServic -> (업로드/썸네일/조회/리액션/검색/조회수) 역할별 서비스로 분리함.
 */
@Service
@RequiredArgsConstructor
public class ShortsService {

    private final ShortsCommandService shortsCommandService;
    private final ShortsQueryService shortsQueryService;
    private final ShortsReactionService shortsReactionService;
    private final ShortsSearchRepository shortsSearchRepository;
    private final ShortsMyQueryService shortsMyQueryService;

    // 쇼츠 생성
    public String createShorts(ShortsCreateRequest request, MultipartFile videoFile,
                               MultipartFile thumbnailFile, String uid)
            throws IOException, ExecutionException, InterruptedException {
        return shortsCommandService.createShorts(request, videoFile, thumbnailFile, uid);
    }

    // 쇼츠 단일 조회
    public ShortsResponse getShorts(String shortsId, String currentUid)
            throws ExecutionException, InterruptedException {
        return shortsQueryService.getShorts(shortsId, currentUid);
    }

    // 최신 쇼츠 피드 조회
    public List<ShortsResponse> getLatestShorts(int limit, String currentUid)
            throws ExecutionException, InterruptedException {
        return shortsQueryService.getLatestShorts(limit, currentUid);
    }

    // 좋아요
    public void likeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        shortsReactionService.likeShorts(shortsId, uid);
    }

    // 좋아요 취소
    public void unlikeShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        shortsReactionService.unlikeShorts(shortsId, uid);
    }

    // 북마크 추가
    public void bookmarkShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        shortsReactionService.bookmarkShorts(shortsId, uid);
    }

    // 북마크 제거
    public void unbookmarkShorts(String shortsId, String uid)
            throws ExecutionException, InterruptedException {
        shortsReactionService.unbookmarkShorts(shortsId, uid);
    }

    // 해시태그 검색 (Firestore)
    public List<ShortsResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {
        return shortsQueryService.searchByHashtag(tag, limit, currentUid);
    }

    // 쇼츠 수정
    public ShortsResponse updateShorts(String shortsId, ShortsUpdateRequest request,
                                       MultipartFile videoFile, MultipartFile thumbnailFile,
                                       String currentUid) throws Exception {
        return shortsCommandService.updateShorts(shortsId, request, videoFile, thumbnailFile, currentUid);
    }

    // 쇼츠 삭제
    public void deleteShorts(String shortsId, String currentUid) throws Exception {
        shortsCommandService.deleteShorts(shortsId, currentUid);
    }

    //내가 누른 좋아요 쇼츠
    public List<ShortsResponse> getLikedShorts(String uid) 
            throws ExecutionException, InterruptedException{
        return shortsMyQueryService.getLikedShorts(uid);
    }

    //내가 누른 북마크 쇼츠
    public List<ShortsResponse> getBookmarkedShorts(String uid)
            throws ExecutionException, InterruptedException{
        return shortsMyQueryService.getBookmarkedShorts(uid);
    }

    /*
        엘라스틱 서치 기반 검색
    */
    public List<ShortsResponse> search(String keyword, int size, String currentUid)
            throws ExecutionException, InterruptedException {

        List<String> ids = shortsSearchRepository.searchShortsIds(keyword, size);
        if (ids.isEmpty()) return List.of();

        List<ShortsResponse> result = new java.util.ArrayList<>();
        for (String shortsId : ids) {
            result.add(shortsQueryService.getShorts(shortsId, currentUid));
        }
        return result;
    }
}
