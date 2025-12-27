package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.petsitter.dto.PetsitterCardDTO;
import com.op.back.petsitter.dto.PetsitterRegisterDTO;
import com.op.back.petsitter.dto.PetsitterRegisterRequestDTO;
import com.op.back.petsitter.dto.TmapDTO;
import com.op.back.petsitter.entity.PetsitterEntity;
import com.op.back.petsitter.repository.PetsitterRepository;
import com.op.back.petsitter.util.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PetsitterService {

    private final TmapService tmapService;
    private final PetsitterRepository petsitterRepository;
    private final Firestore firestore;

    public List<PetsitterCardDTO> getPetsitters(
            String Address,
            String petType,
            Integer minPrice,
            Integer maxPrice,
            Boolean reserveAvailable,
            Boolean verified,
            String sort,
            Double userLat,
            Double userLng
    ) {

        List<PetsitterEntity> petsitters =
                petsitterRepository.findPetsitters(
                        Address, petType, reserveAvailable, verified
                );

        return petsitters.stream()
                // 가격 필터
                .filter(p -> filterPrice(p, minPrice, maxPrice))
                // DTO 변환 + 거리 계산
                .map(p -> toDTO(p, userLat, userLng))
                // 정렬
                .sorted(getComparator(sort))
                .toList();
    }

    public void registerPetsitter(
            String uid,
            PetsitterRegisterRequestDTO register
    ) {
        DocumentReference userRef =
                firestore.collection("users").document(uid);

        DocumentSnapshot user = null;
        try {
            user = userRef.get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        // 1. 유저 존재 확인
        if (!user.exists()) {
            throw new IllegalStateException("유저가 존재하지 않습니다.");
        }

        // 2. 펫시터 권한 확인 (핵심)
        Boolean isPetsitter = user.getBoolean("petsitter");
        if (isPetsitter == null || !isPetsitter) {
            throw new IllegalStateException("펫시터 권한이 없는 사용자입니다.");
        }

        TmapDTO tmapDTO = null;
        try {
            tmapDTO = tmapService.convertAddressToLatLng(register.getAddress());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 3. 펫시터 정보 저장
        PetsitterRegisterDTO petsitter = new PetsitterRegisterDTO(
                uid,
                register.getName(),
                register.getAddress(),
                tmapDTO.getLat(),
                tmapDTO.getLng(),
                register.getCaption(),
                register.getCareer(),
                register.getPrice(),
                register.isDog(),
                register.isCat(),
                register.isEtc()
        );

        firestore.collection("petsitters")
                .document(uid)
                .set(petsitter);
    }

    private boolean filterPrice(PetsitterEntity p, Integer min, Integer max) {
        if (min != null && p.getPrice() < min) return false;
        if (max != null && p.getPrice() > max) return false;
        return true;
    }

    private PetsitterCardDTO toDTO(PetsitterEntity p, Double lat, Double lng) {
        double distance = 0.0;
        if (lat != null && lng != null) {
            distance = DistanceUtil.calculate(lat, lng, p.getLat(), p.getLng());
            distance = Math.round(distance * 10) / 10.0;
        }

        return new PetsitterCardDTO(
                p.getPetsitterId(),
                p.getName(),
                p.getProfileImageUrl(),
                p.getAddress(),
                distance,
                p.getRating(),
                p.getMannerTemp(),
                p.getCaption(),
                p.isVerified(),
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

    private Comparator<PetsitterCardDTO> getComparator(String sort) {
        if (sort == null) return Comparator.comparing(PetsitterCardDTO::getDistance);

        return switch (sort) {
            case "rating" -> Comparator.comparing(PetsitterCardDTO::getRating).reversed();
            case "price" -> Comparator.comparing(PetsitterCardDTO::getPrice);
            default -> Comparator.comparing(PetsitterCardDTO::getDistance);
        };
    }
}

