package com.op.back.lecture.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.auth.model.User;
import com.op.back.lecture.dto.LectureReviewCreateRequest;
import com.op.back.lecture.dto.LectureReviewListResponse;
import com.op.back.lecture.dto.LectureReviewResponse;
import com.op.back.lecture.dto.LectureReviewUpdateRequest;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.LectureReview;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.lecture.repository.LectureReviewRepository;
import com.op.back.lecture.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LectureReviewServiceImpl implements LectureReviewService {

    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final LectureReviewRepository lectureReviewRepository;

    @Override
    public LectureReviewResponse create(String lectureId, LectureReviewCreateRequest req, String currentUid) {

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        User user = userRepository.findByUid(currentUid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        // docId = uid 이므로 중복은 transaction에서 막히지만, 빠른 실패를 위해 한번 더
        if (lectureReviewRepository.existsReview(lectureId, currentUid)) {
            throw new IllegalStateException("이미 리뷰를 작성했습니다.");
        }

        LectureReview review = new LectureReview();
        review.setUid(currentUid);
        review.setNickname(user.getNickname());
        review.setRating(req.rating());
        review.setContent(req.content());
        review.setCreatedAt(Timestamp.now());
        review.setUpdatedAt(Timestamp.now());

        lectureReviewRepository.createReview(lectureId, currentUid, review, lecture.getTitle());

        return toResponse(review, currentUid);
    }

    @Override
    public LectureReviewListResponse list(String lectureId, int limit, int offset, String currentUid) {

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        List<LectureReview> reviews = lectureReviewRepository.findReviews(lectureId, limit, offset);

        List<LectureReviewResponse> items = reviews.stream()
                .map(r -> toResponse(r, currentUid))
                .toList();

        return new LectureReviewListResponse(
                lecture.getRating(),
                lecture.getReviewCount(),
                items
        );
    }

    @Override
    public LectureReviewResponse getMine(String lectureId, String currentUid) {
        LectureReview review = lectureReviewRepository.findReview(lectureId, currentUid)
                .orElseThrow(() -> new IllegalStateException("작성한 리뷰가 없습니다."));
        return toResponse(review, currentUid);
    }

    @Override
    public List<LectureReviewResponse> getMyReviews(String uid) {

        return lectureReviewRepository.findMyReviews(uid)
                .stream()
                .map(review -> LectureReviewResponse.from(
                        review,
                        true // mine = true
                ))
                .toList();
    }
    
    @Override
    public LectureReviewResponse update(String lectureId, LectureReviewUpdateRequest req, String currentUid) {

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        User user = userRepository.findByUid(currentUid)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        LectureReview existing = lectureReviewRepository.findReview(lectureId, currentUid)
                .orElseThrow(() -> new IllegalStateException("작성한 리뷰가 없습니다."));

        LectureReview review = new LectureReview();
        review.setUid(currentUid);
        review.setNickname(user.getNickname()); // 닉네임 스냅샷 갱신 여부는 정책. 여기선 최신으로 반영.
        review.setRating(req.rating());
        review.setContent(req.content());
        review.setCreatedAt(existing.getCreatedAt()); // 유지
        review.setUpdatedAt(Timestamp.now());

        lectureReviewRepository.updateReview(lectureId, currentUid, review, lecture.getTitle());

        return toResponse(review, currentUid);
    }

    @Override
    public void delete(String lectureId, String currentUid) {
        lectureReviewRepository.deleteReview(lectureId, currentUid);
    }

    private LectureReviewResponse toResponse(LectureReview r, String currentUid) {
        Instant created = r.getCreatedAt() != null ? r.getCreatedAt().toDate().toInstant() : null;
        Instant updated = r.getUpdatedAt() != null ? r.getUpdatedAt().toDate().toInstant() : null;
        boolean mine = r.getUid() != null && r.getUid().equals(currentUid);

        return new LectureReviewResponse(
                r.getUid(),
                r.getNickname(),
                r.getRating(),
                r.getContent(),
                created,
                updated,
                mine
        );
    }
}
