package com.op.back.petsitter.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.op.back.petsitter.dto.PetsitterOperateTimeDTO;
import com.op.back.petsitter.dto.PetsitterRegisterDTO;
import com.op.back.petsitter.dto.PetsitterRegisterRequestDTO;
import com.op.back.petsitter.dto.PetsitterUpdateRequestDTO;
import com.op.back.petsitter.dto.TmapDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PetsitterCommandService {

    private final TmapService tmapService;
    private final Firestore firestore;

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
            throw new IllegalStateException("펫시터 권한이 없습니다.");
        }
        // users 컬렉션에서 전화번호 가져오기
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

    public void updatePetsitter(String uid, PetsitterUpdateRequestDTO request) {
        DocumentReference petSitterRef = firestore.collection("petsitters").document(uid);

        DocumentSnapshot snapshot;
        try {
            snapshot = petSitterRef.get().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!snapshot.exists()) {
            throw new IllegalStateException("펫시터 정보가 존재하지 않습니다.");
        }

        Map<String, Object> updates = new HashMap<>();

        if (request.getAddress() != null) {
            TmapDTO coord = tmapService.convertAddressToLatLng(request.getAddress());

            updates.put("address", request.getAddress());
            updates.put("lat", coord.getLat());
            updates.put("lng", coord.getLng());
        }
        if (request.getPhone() != null) {
            updates.put("phone", request.getPhone());
        }
        if (request.getCaption() != null) {
            updates.put("caption", request.getCaption());
        }
        if (request.getCareer() != null) {
            updates.put("career", request.getCareer());
        }
        if (request.getPrice() != null) {
            updates.put("price", request.getPrice());
        }

        if (request.getDog() != null) {
            updates.put("dog", request.getDog());
        }
        if (request.getCat() != null) {
            updates.put("cat", request.getCat());
        }
        if (request.getEtc() != null) {
            updates.put("etc", request.getEtc());
        }

        if (request.getReserveAvailable() != null) {
            updates.put("reserveAvailable", request.getReserveAvailable());
        }

        if (updates.isEmpty()) {
            throw new IllegalStateException("수정할 내용이 없습니다.");
        }

        petSitterRef.update(updates);
    }

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
}
