package com.op.back.petsitter.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.petsitter.dto.PetsitterCardDTO;
import com.op.back.petsitter.dto.PetsitterRegisterDTO;
import com.op.back.petsitter.dto.PetsitterRegisterRequestDTO;
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

    @GetMapping
    public ResponseEntity<List<PetsitterCardDTO>> getPetsitters(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String petType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean reserveAvailable,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String sort,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return ResponseEntity.ok(
                petsitterService.getPetsitters(
                        region, petType, minPrice, maxPrice,
                        reserveAvailable, verified, sort, lat, lng
                )
        );
    }
    @PostMapping("/register")
    public ResponseEntity<Void> registerPetsitter(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PetsitterRegisterRequestDTO request
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));
        petsitterService.registerPetsitter(uid, request);
        return ResponseEntity.ok().build();
    }

}
