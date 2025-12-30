package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.op.back.petsitter.dto.*;
import com.op.back.petsitter.entity.PetsitterEntity;
import com.op.back.petsitter.repository.PetsitterRepository;
import com.op.back.petsitter.util.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
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
                .map(p -> toDTO(p, userLat, userLng))
                // 정렬
                .sorted(getComparator(sort))
                .toList();
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

        return toDTO(entity, lat, lng); // 거리 계산 X
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
        // users 컬렉션에서 폰 번호 가져오기
        String phone = user.getString("phone");

        TmapDTO tmapDTO = null;
        try {
            tmapDTO = tmapService.convertAddressToLatLng(register.getAddress());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 3. 펫시터 정보 저장
        PetsitterRegisterDTO petsitter = new PetsitterRegisterDTO(
                uid,
                user.getString("name"),
                user.getString("profileImageUrl"),
                register.getAddress(),
                phone,
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

    private Comparator<PetsitterCardDTO> getComparator(String sort) {
        if (sort == null) return Comparator.comparing(PetsitterCardDTO::getDistance);

        return switch (sort) {
            case "rating" -> Comparator.comparing(PetsitterCardDTO::getRating).reversed();
            case "price" -> Comparator.comparing(PetsitterCardDTO::getPrice);
            default -> Comparator.comparing(PetsitterCardDTO::getDistance);
        };
    }

    public void updatePetsitter(String uid, PetsitterUpdateRequestDTO request) {
        DocumentReference petSitterRef = firestore.collection("petsitters").document(uid);

        DocumentSnapshot snapshot;
        try{
            snapshot = petSitterRef.get().get();
        }catch(Exception e){
            throw new RuntimeException(e);
        }

        if(!snapshot.exists()){
            throw new IllegalStateException("펫시터 정보가 존재하지 않습니다.");
        }

        Map<String, Object> updates = new HashMap<>();

        if(request.getAddress() != null){
            TmapDTO coord = tmapService.convertAddressToLatLng(request.getAddress());

            updates.put("address", request.getAddress());
            updates.put("lat", coord.getLat());
            updates.put("lng", coord.getLng());
        }
        if(request.getPhone() != null){updates.put("phone", request.getPhone());}
        if(request.getCaption() != null){updates.put("caption", request.getCaption());}
        if(request.getCareer() != null){updates.put("career", request.getCareer());}
        if(request.getPrice() != null){updates.put("price", request.getPrice());}

        if(request.getDog() != null){updates.put("dog", request.getDog());}
        if(request.getCat() != null){updates.put("cat", request.getCat());}
        if(request.getEtc() != null){updates.put("etc", request.getEtc());}

        if(request.getReserveAvailable() != null){updates.put("reserveAvailable", request.getReserveAvailable());}

        if(updates.isEmpty()){
            throw new IllegalStateException("수정할 내용이 없습니다.");
        }

        petSitterRef.update(updates);
    }

    // 운영 시간 변경
    public void updateOperatingTime(
            String petsitterId,
            Map<String, PetsitterOperateTimeDTO> operatingTime
    ) {

        Map<String, Object> saveMap = new HashMap<>();

        for (Map.Entry<String, PetsitterOperateTimeDTO> entry
                : operatingTime.entrySet()) {

            String day = entry.getKey();
            PetsitterOperateTimeDTO dto = entry.getValue();

            if (dto == null) {
                saveMap.put(day, null);
                continue;
            }

            saveMap.put(day, Map.of(
                    "start", dto.getStart(),
                    "end", dto.getEnd()
            ));
        }

        firestore.collection("petsitters")
                .document(petsitterId)
                .update("operatingTime", saveMap);
    }


    // 운영 시간 조회
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

}

