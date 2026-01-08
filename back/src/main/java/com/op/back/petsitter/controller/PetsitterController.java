package com.op.back.petsitter.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.petsitter.dto.*;
import com.op.back.petsitter.service.PetsitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/petsitters")
@RequiredArgsConstructor
public class PetsitterController {

    private final PetsitterService petsitterService;
    private final JwtUtil jwtUtil;

    // 거리 기준 모든 펫시터 조회
    @GetMapping
    public ResponseEntity<List<PetsitterCardDTO>> getPetsitters(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String petType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean reserveAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return ResponseEntity.ok(
                petsitterService.getPetsitters(
                        region, petType, minPrice, maxPrice,
                        reserveAvailable, sort, lat, lng
                )
        );
    }

    // 펫시터 개별정보 조회
    @GetMapping("/{petsitterId}")
    public ResponseEntity<PetsitterCardDTO> getPetsitter(
            @PathVariable String petsitterId,
            @RequestParam Double lat,
            @RequestParam Double lng
    ){
        return ResponseEntity.ok(
                petsitterService.getPetsitter(petsitterId, lat, lng)
        );
    }

    // 펫시터 정보 등록
    @PostMapping("/register")
    public ResponseEntity<Void> registerPetsitter(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PetsitterRegisterRequestDTO request
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));
        petsitterService.registerPetsitter(uid, request);
        return ResponseEntity.ok().build();
    }

    // 펫시터 정보 유무 조회
    @GetMapping("/register/exists")
    public ResponseEntity<PetsitterExistsResponseDTO> checkPetsittersExists(
            @RequestHeader("Authorization") String authHeader
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));
        return ResponseEntity.ok(
          petsitterService.existsPetsitter(uid)
        );
    }

    @GetMapping("/operatingTime/{petsitterId}")
    public ResponseEntity<OperatingTimeResponseDTO> getOperatingTime(
            @PathVariable String petsitterId
    ){
        return ResponseEntity.ok(
                petsitterService.getOperatingTime(petsitterId)
        );
    }

    // 펫시터 운영 시간 등록 또는 업데이트
    @PatchMapping("/operatingTime")
    public ResponseEntity<Void> updateOperatingTime(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OperatingTimeUpdateRequestDTO request
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));
        petsitterService.updateOperatingTime(uid, request.getOperatingTime());
        return ResponseEntity.ok().build();
    }

    // 펫시터 정보 수정
    @PatchMapping("/update")
    public ResponseEntity<List<PetsitterUpdateRequestDTO>> updatePetsitters(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PetsitterUpdateRequestDTO request
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));
        petsitterService.updatePetsitter(uid, request);
        return ResponseEntity.ok().build();
    }
}
