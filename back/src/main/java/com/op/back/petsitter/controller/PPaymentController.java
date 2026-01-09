package com.op.back.petsitter.controller;


import com.op.back.petsitter.dto.PaymentCancelRequestDTO;
import com.op.back.petsitter.dto.PaymentReadyRequestDTO;
import com.op.back.petsitter.dto.PaymentReadyResponseDTO;
import com.op.back.petsitter.dto.PaymentSuccessRequestDTO;
import com.op.back.petsitter.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/petsitters/payment")
@RequiredArgsConstructor
public class PPaymentController {
    private final PaymentService paymentService;

    // ready 이후 사용자가 결제 완료 할 것인지 취소할 것인지 선택해야함

    @PostMapping("/ready")
    public ResponseEntity<?> ready(
            @RequestBody PaymentReadyRequestDTO req
    ) {
        PaymentReadyResponseDTO dto = paymentService.readyPayment(req.getReservationId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                "paymentId", dto.getPaymentId(),
                "amount", dto.getAmount(),
                "expiredAt", dto.getExpireAt()
        ));
    }

    @PostMapping("/success")
    public ResponseEntity<String> success(
            @RequestBody PaymentSuccessRequestDTO req
    ) {
        try {
            paymentService.paymentSuccess(req.getReservationId(), req.getPaymentId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("success");
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(
            @RequestBody PaymentCancelRequestDTO req
    ) {
        try {
            paymentService.paymentCancel(req.getReservationId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("cancel");
    }
}
