package com.op.back.common.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class FirestoreDeleteUtil {
    private final Firestore firestore;

    //Firestore 문서 + 모든 하위 컬렉션 재귀 삭제
    public void deleteDocumentWithSubcollections(DocumentReference docRef)
            throws ExecutionException, InterruptedException {

        // 1) 하위 컬렉션 조회
        List<CollectionReference> subCollections = (List<CollectionReference>) docRef.listCollections();
        for (CollectionReference colRef : subCollections) {
            deleteCollection(colRef, 50);
        }

        // 2) 마지막에 자기 자신 삭제
        docRef.delete().get();
    }



    //컬렉션 내부 문서 전체 삭제 (batch)
    private void deleteCollection(CollectionReference collection, int batchSize)
            throws ExecutionException, InterruptedException {

        ApiFuture<QuerySnapshot> future = collection.limit(batchSize).get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        if (docs.isEmpty()) return;

        WriteBatch batch = firestore.batch();
        for (QueryDocumentSnapshot doc : docs) {
            // 문서 내부 하위 컬렉션까지 재귀 삭제
            deleteDocumentWithSubcollections(doc.getReference());
            batch.delete(doc.getReference());
        }
        batch.commit().get();

        // 아직 남아있으면 재귀 호출
        deleteCollection(collection, batchSize);
    }
}