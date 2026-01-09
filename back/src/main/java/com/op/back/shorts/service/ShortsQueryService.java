package com.op.back.shorts.service;

import com.google.cloud.firestore.*;
import com.op.back.shorts.dto.ShortsResponse;
import com.op.back.shorts.model.Shorts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

//조회/리스트/해시태그(Firestore)
@Service
@RequiredArgsConstructor
public class ShortsQueryService {

    private final Firestore firestore;
    private final ShortsMapperService shortsMapperService;
    private final ShortsReactionService shortsReactionService;
    private final ShortsViewService shortsViewService;

    private static final String SHORTS = "shorts";

    // 쇼츠 단일 조회
    public ShortsResponse getShorts(String shortsId, String currentUid)
            throws ExecutionException, InterruptedException {

        DocumentSnapshot doc = firestore.collection(SHORTS)
                .document(shortsId)
                .get()
                .get();

        if (!doc.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        shortsViewService.handleViewCount(shortsId, currentUid);

        Shorts s = shortsMapperService.toShorts(doc);
        boolean liked = shortsReactionService.isLiked(shortsId, currentUid);
        boolean bookmarked = shortsReactionService.isBookmarked(shortsId, currentUid);

        return shortsMapperService.toResponse(s, liked, bookmarked, currentUid);
    }

    // 최신 쇼츠 피드 조회
    public List<ShortsResponse> getLatestShorts(int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        List<QueryDocumentSnapshot> docs = firestore.collection(SHORTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get()
                .getDocuments();

        List<ShortsResponse> result = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            Shorts s = shortsMapperService.toShorts(doc);
            boolean liked = shortsReactionService.isLiked(s.getId(), currentUid);
            boolean bookmarked = shortsReactionService.isBookmarked(s.getId(), currentUid);

            result.add(shortsMapperService.toResponse(s, liked, bookmarked, currentUid));
        }
        return result;
    }

    // 해시태그 검색
    public List<ShortsResponse> searchByHashtag(String tag, int limit, String currentUid)
            throws ExecutionException, InterruptedException {

        Query query = firestore.collection(SHORTS)
                .whereArrayContains("hashtags", tag)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit);

        List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

        List<ShortsResponse> result = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Shorts s = shortsMapperService.toShorts(doc);
            boolean liked = shortsReactionService.isLiked(s.getId(), currentUid);
            boolean bookmarked = shortsReactionService.isBookmarked(s.getId(), currentUid);
            result.add(shortsMapperService.toResponse(s, liked, bookmarked, currentUid));
        }
        return result;
    }
}
