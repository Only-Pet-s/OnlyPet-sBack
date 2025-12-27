package com.op.back.petsitter.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.op.back.petsitter.entity.PetsitterEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PetsitterRepository {

    private final Firestore firestore;

    public List<PetsitterEntity> findPetsitters(
            String region,
            String petType,
            Boolean reserveAvailable,
            Boolean verified
    ) {
        Query query = firestore.collection("petsitters");

        if (region != null) {
            query = query.whereEqualTo("region", region);
        }

        if (reserveAvailable != null) {
            query = query.whereEqualTo("reserveAvailable", reserveAvailable);
        }

        if (verified != null) {
            query = query.whereEqualTo("verified", verified);
        }

        if (petType != null) {
            switch (petType) {
                case "DOG" -> query = query.whereEqualTo("dog", true);
                case "CAT" -> query = query.whereEqualTo("cat", true);
                case "ETC" -> query = query.whereEqualTo("etc", true);
            }
        }

        try {
            return query.get().get()
                    .getDocuments()
                    .stream()
                    .map(doc -> doc.toObject(PetsitterEntity.class))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("펫시터 목록 조회 실패", e);
        }
    }
}
