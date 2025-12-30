package com.op.back.petsitter.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.petsitter.dto.ReservationRequestDTO;
import com.op.back.petsitter.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/petsitters/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/makeReservation")
    public ResponseEntity<?> makeReservation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReservationRequestDTO req
        ){
            try {
                String uid = jwtUtil.getUid(authHeader.substring(7));
                String reservationId =
                        reservationService.createReservation(uid, req);

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("reservationId", reservationId));

            } catch (IllegalStateException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
        }
    }
}
