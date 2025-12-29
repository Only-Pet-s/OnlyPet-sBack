package com.op.back.petsitter.controller;


import com.op.back.petsitter.dto.TmapDTO;
import com.op.back.petsitter.service.TmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/petsitters/tmap")
@RequiredArgsConstructor
public class TmapController {

    private final TmapService tmapService;

    @GetMapping
    public ResponseEntity<TmapDTO> getTmap(
            @RequestParam String address
    ) throws Exception {

        TmapDTO result = tmapService.convertAddressToLatLng(address);
        return ResponseEntity.ok(result);
    }
}
