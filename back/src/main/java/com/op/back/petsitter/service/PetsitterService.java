package com.op.back.petsitter.service;

import com.op.back.petsitter.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetsitterService {

    private final PetsitterCommandService petsitterCommandService;
    private final PetsitterQueryService petsitterQueryService;

    public List<PetsitterCardDTO> getPetsitters(
            String uid,
            String Address,
            String petType,
            Integer minPrice,
            Integer maxPrice,
            Boolean reserveAvailable,
            String sort,
            Double userLat,
            Double userLng
    ) {
        return petsitterQueryService.getPetsitters(
                uid,
                Address,
                petType,
                minPrice,
                maxPrice,
                reserveAvailable,
                sort,
                userLat,
                userLng
        );
    }

    public PetsitterCardDTO getMyInfoPs(String petsitterId) {
        return petsitterQueryService.getMyInfoPs(petsitterId);
    }

    public PetsitterCardDTO getPetsitter(String petsitterId, Double lat, Double lng) {
        return petsitterQueryService.getPetsitter(petsitterId, lat, lng);
    }

    public void registerPetsitter(String uid, PetsitterRegisterRequestDTO register) {
        petsitterCommandService.registerPetsitter(uid, register);
    }

    public PetsitterExistsResponseDTO existsPetsitter(String uid) {
        return petsitterQueryService.existsPetsitter(uid);
    }

    public void updatePetsitter(String uid, PetsitterUpdateRequestDTO request) {
        petsitterCommandService.updatePetsitter(uid, request);
    }

    public void updateOperatingTime(
            String petsitterId,
            Map<String, PetsitterOperateTimeDTO> operatingTime
    ) {
        petsitterCommandService.updateOperatingTime(petsitterId, operatingTime);
    }

    public OperatingTimeResponseDTO getOperatingTime(String petsitterId) {
        return petsitterQueryService.getOperatingTime(petsitterId);
    }
}
