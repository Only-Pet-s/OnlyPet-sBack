package com.op.back.petsitter.controller;

import com.op.back.petsitter.service.PetsitterReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/petsitters/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final PetsitterReviewService psReviewService;
}
