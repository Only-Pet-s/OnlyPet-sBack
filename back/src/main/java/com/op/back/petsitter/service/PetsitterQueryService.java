package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.petsitter.dto.OperatingTimeResponseDTO;
import com.op.back.petsitter.dto.PetsitterCardDTO;
import com.op.back.petsitter.dto.PetsitterExistsResponseDTO;
import com.op.back.petsitter.dto.PetsitterOperateTimeDTO;
import com.op.back.petsitter.entity.PetsitterEntity;
import com.op.back.petsitter.repository.PetsitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PetsitterQueryService {

    private final PetsitterRepository petsitterRepository;
    private final Firestore firestore;
    private final PetsitterMapperService petsitterMapperService;

    public List<PetsitterCardDTO> getPetsitters(
            String Address,
            String petType,
            Integer minPrice,
            Integer maxPrice,
            Boolean reserveAvailable,
            String sort,
            Double userLat,
            Double userLng
    ) {

        List<PetsitterEntity> petsitters =
                petsitterRepository.findPetsitters(
                        Address, petType, reserveAvailable
                );

        return petsitters.stream()
                // 가격 필터
                .filter(p -> filterPrice(p, minPrice, maxPrice))
                // DTO 변환 + 거리 계산
                .map(p -> petsitterMapperService.toDTO(p, userLat, userLng))
                // 정렬
                .sorted(getComparator(sort))
                .toList();
    }

    public PetsitterCardDTO getMyInfoPs(String petsitterId) {
        DocumentSnapshot snap = null;
        try {
            snap = firestore.collection("petsitters")
                    .document(petsitterId)
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!snap.exists()) {
            throw new IllegalStateException("펫시터를 찾을 수 없습니다.");
        }
        PetsitterEntity entity =
                snap.toObject(PetsitterEntity.class);

        return petsitterMapperService.toDTO(entity, snap.getDouble("lat"), snap.getDouble("lng"));
    }

    public PetsitterCardDTO getPetsitter(String petsitterId, Double lat, Double lng) {
        DocumentSnapshot snapshot = null;
        try {
            snapshot = firestore.collection("petsitters")
                    .document(petsitterId)
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!snapshot.exists()) {
            throw new IllegalStateException("펫시터를 찾을 수 없습니다.");
        }

        PetsitterEntity entity =
                snapshot.toObject(PetsitterEntity.class);

        return petsitterMapperService.toDTO(entity, lat, lng); // 거리 계산 X
    }

    public PetsitterExistsResponseDTO existsPetsitter(String uid) {

        DocumentSnapshot snapshot;
        try {
            snapshot = firestore.collection("petsitters")
                    .document(uid)
                    .get().get();
        } catch (Exception e) {
            throw new RuntimeException("펫시터 조회 실패", e);
        }

        return new PetsitterExistsResponseDTO(snapshot.exists());
    }

    public OperatingTimeResponseDTO getOperatingTime(String petsitterId) {

        DocumentSnapshot snapshot = null;
        try {
            snapshot = firestore.collection("petsitters")
                    .document(petsitterId)
                    .get().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!snapshot.exists()) {
            throw new IllegalStateException("펫시터가 존재하지 않습니다.");
        }

        Map<String, Object> raw =
                (Map<String, Object>) snapshot.get("operatingTime");

        Map<String, PetsitterOperateTimeDTO> result = new HashMap<>();

        if (raw != null) {
            for (Map.Entry<String, Object> entry : raw.entrySet()) {

                String day = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    result.put(day, null);
                    continue;
                }

                Map<String, Object> timeMap = (Map<String, Object>) value;

                result.put(
                        day,
                        new PetsitterOperateTimeDTO(
                                (String) timeMap.get("start"),
                                (String) timeMap.get("end")
                        )
                );
            }
        }

        return new OperatingTimeResponseDTO(petsitterId, result);
    }

    private boolean filterPrice(PetsitterEntity p, Integer min, Integer max) {
        if (min != null && p.getPrice() < min) return false;
        if (max != null && p.getPrice() > max) return false;
        return true;
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
