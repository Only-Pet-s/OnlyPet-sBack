package com.op.back.petsitter.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.petsitter.dto.*;
import com.op.back.petsitter.service.PetsitterService;
import com.op.back.petsitter.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/petsitters/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;
    private final PetsitterService petsitterService;

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

    @GetMapping("/getAvailableT/{petsitterId}")
    public ResponseEntity<?> getAvailableT(
            @PathVariable String petsitterId,
            @RequestParam String date
    ){
        return ResponseEntity.ok(
                reservationService.getAvailableTimes(petsitterId, date)
        );
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<?> cancel(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String reservationId
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));

        CancelReservationResponseDTO res = null;
        try {
            res = reservationService.cancelReservation(reservationId, uid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(res);
    }

    @GetMapping("/myReserve")
    public ResponseEntity<List<ReadUserReservationDTO>> getMyReserve(
            @RequestHeader("Authorization") String authHeader
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));

        List<ReadUserReservationDTO> result = reservationService.getUserReservation(uid);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/getReserved")
    public ResponseEntity<List<ReadPetsitterReservedDTO>> getReserved(
            @RequestHeader("Authorization") String authHeader
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));

        List<ReadPetsitterReservedDTO> result = reservationService.getPetsitterReserved(uid);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/getCount/{petsitterId}")
    public ResponseEntity<PetsitterReservationCountDTO> getCount(
            @PathVariable("petsitterId") String petsitterId
    ){

        return ResponseEntity.ok(
           reservationService.getReservationCount(petsitterId)
        );
    }

    @GetMapping("/revenue")
    public ResponseEntity<PetsitterRevenueDTO> getRevenue(
            @RequestHeader("Authorization") String authHeader
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));
        return ResponseEntity.ok(
            reservationService.getTotalRevenue(uid)
        );
    }

    @GetMapping("/schedule")
    public ResponseEntity<ScheduleWeekDTO> getWeekSchedule(
            @RequestHeader("Authorization") String authHeader
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));
        return ResponseEntity.ok(
                reservationService.getScheduleWeek(uid)
        );
    }
}
