package com.op.back.petsitter.service;

import com.op.back.petsitter.dto.PetsitterCardDTO;
import com.op.back.petsitter.entity.PetsitterEntity;
import com.op.back.petsitter.util.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetsitterMapperService {

    public PetsitterCardDTO toDTO(PetsitterEntity p, Double lat, Double lng) {

        double distance = 0.0;

        if (lat != null && lng != null) {
            distance = DistanceUtil.calculate(lat, lng, p.getLat(), p.getLng());
            distance = Math.round(distance * 10) / 10.0;
        }

        return new PetsitterCardDTO(
                p.getPetsitterId(),
                p.getName(),
                p.getProfileImageUrl(),
                p.getPhone(),
                p.getAddress(),
                distance,
                p.getRating(),
                p.getMannerTemp(),
                p.getCaption(),
                p.getCareer(),
                p.getCompleteCount(),
                p.getResponseRatio(),
                p.getPrice(),
                p.isReserveAvailable(),
                p.isDog(),
                p.isCat(),
                p.isEtc()
        );
    }
}
