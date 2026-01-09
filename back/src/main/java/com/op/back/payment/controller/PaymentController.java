package com.op.back.payment.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.op.back.payment.dto.LecturePurchaseRequest;
import com.op.back.payment.dto.PurchaseResponse;
import com.op.back.payment.dto.SubscriptionPurchaseRequest;
import com.op.back.payment.dto.SubscriptionResponse;
import com.op.back.payment.service.PurchaseService;
import com.op.back.payment.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PurchaseService purchaseService;
    private final SubscriptionService subscriptionService;

    private String currentUid() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/lectures")
    public PurchaseResponse purchaseLecture(@RequestBody LecturePurchaseRequest request) {
        return purchaseService.purchaseLecture(currentUid(), request);
    }

    @PostMapping("/subscriptions")
    public SubscriptionResponse subscribe(@RequestBody SubscriptionPurchaseRequest request) {
        return subscriptionService.subscribe(currentUid(), request);
    }

    @GetMapping("/my/purchases")
    public List<PurchaseResponse> myPurchases() {
        return purchaseService.getMyPurchases(currentUid());
    }

    @GetMapping("/my/subscriptions")
    public List<SubscriptionResponse> mySubscriptions() {
        return subscriptionService.getMySubscriptions(currentUid());
    }
}
