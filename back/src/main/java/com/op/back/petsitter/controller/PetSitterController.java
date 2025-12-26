package com.op.back.petsitter.controller;

import com.op.back.petsitter.dto.PetsitterCardDTO;
import com.op.back.petsitter.service.PetSitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/petsitter")
@RequiredArgsConstructor
public class PetSitterController {
    private final PetSitterService petSitterService;

    @GetMapping
    public ResponseEntity<List<PetsitterCardDTO>> getPetsitters(
            
    ){
        return null;
    }
}
