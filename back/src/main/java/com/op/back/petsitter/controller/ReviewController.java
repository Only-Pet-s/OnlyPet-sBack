package com.op.back.petsitter.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.petsitter.dto.ReviewRequestDTO;
import com.op.back.petsitter.service.PetsitterReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/petsitters/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final PetsitterReviewService psReviewService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{petsitterId}")
    public ResponseEntity<Void> createReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String petsitterId,
            @RequestBody ReviewRequestDTO req
            )throws Exception{
        String uid = jwtUtil.getUid(authHeader.substring(7));
        psReviewService.createReview(uid,petsitterId,req);

        return ResponseEntity.ok().build();
    }
}
