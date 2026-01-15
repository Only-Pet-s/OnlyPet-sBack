package com.op.back.petsitter.service;

import com.op.back.petsitter.dto.PetsitterReviewResponseDTO;
import com.op.back.petsitter.dto.ReviewRequestDTO;
import com.op.back.petsitter.dto.ReviewUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetsitterReviewService {

    private final PetsitterReviewCommandService petsitterReviewCommandService;
    private final PetsitterReviewQueryService petsitterReviewQueryService;

    public void createReview(
            String uid,
            String petsitterId,
            ReviewRequestDTO req
    ) {
        petsitterReviewCommandService.createReview(uid, petsitterId, req);
    }

    public List<PetsitterReviewResponseDTO> getPetsitterReviews(String petsitterId) {
        return petsitterReviewQueryService.getPetsitterReviews(petsitterId);
    }

    public List<PetsitterReviewResponseDTO> getUserReviews(String uid) {
        return petsitterReviewQueryService.getUserReviews(uid);
    }

    public void updateReview(
            String uid,
            String petsitterId,
            String reviewId,
            ReviewUpdateDTO req
    ) {
        petsitterReviewCommandService.updateReview(uid, petsitterId, reviewId, req);
    }

    public void deleteReview(String uid, String petsitterId, String reviewId) {
        petsitterReviewCommandService.deleteReview(uid, petsitterId, reviewId);
    }
}
