package com.op.back.follow.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.follow.dto.FollowUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final Firestore firestore;

    // 팔로우
    public void follow(String myUid, String tUid) {
        if (myUid.equals(tUid)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없음");
        }

        DocumentReference followingRef = firestore
                .collection("users").document(myUid)
                .collection("following").document(tUid);

        DocumentReference followerRef = firestore
                .collection("users").document(tUid)
                .collection("followers").document(myUid);

        DocumentReference userRef = firestore.collection("users").document(myUid);
        DocumentReference tRef = firestore.collection("users").document(tUid);

        try {
            firestore.runTransaction(tx -> {
                if (tx.get(followingRef).get().exists()) {
                    throw new IllegalStateException("이미 팔로우 중");
                }
                tx.set(followingRef, Map.of("createdAt", Timestamp.now()));
                tx.set(followerRef, Map.of("createdAt", Timestamp.now()));

                tx.update(userRef, "followingCount", FieldValue.increment(1));
                tx.update(tRef, "followerCount", FieldValue.increment(1));

                return null;
            }).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // 언팔로우
    public void unfollow(String myUid, String tUid) {
        DocumentReference followingRef = firestore
                .collection("users").document(myUid)
                .collection("following").document(tUid);

        DocumentReference followerRef = firestore
                .collection("users").document(tUid)
                .collection("followers").document(myUid);

        DocumentReference userRef = firestore.collection("users").document(myUid);
        DocumentReference tRef = firestore.collection("users").document(tUid);

        try {

            firestore.runTransaction(transaction -> {

                if (!transaction.get(followingRef).get().exists()) {
                    throw new IllegalStateException("팔로우 상태 아님");
                }

                transaction.delete(followingRef);
                transaction.delete(followerRef);

                transaction.update(userRef, "followingCount", FieldValue.increment(-1));
                transaction.update(tRef, "followerCount", FieldValue.increment(-1));

                return null;
            }).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FollowUserDTO> getFollowers(String uid) {
        return getFollowList(uid, "followers");
    }

    public List<FollowUserDTO> getFollowing(String uid) {
        return getFollowList(uid, "following");
    }

    public boolean isFollowing(String myUid, String targetUid) {
        try{
            DocumentSnapshot snapshot = firestore
                    .collection("users")
                    .document(myUid)
                    .collection("following")
                    .document(targetUid)
                    .get()
                    .get();

            return snapshot.exists();
        }catch (Exception e){
            throw new RuntimeException("팔로우 여부 확인 실패",e);
        }
    }
    // 공통 조회
    private List<FollowUserDTO> getFollowList(String uid, String type) {
        try {
            List<QueryDocumentSnapshot> docs = firestore
                    .collection("users").document(uid)
                    .collection(type)
                    .get().get().getDocuments();

            List<FollowUserDTO> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : docs) {
                String targetUid = doc.getId();
                DocumentSnapshot userDoc = firestore
                        .collection("users").document(targetUid)
                        .get().get();

                result.add(new FollowUserDTO(
                        targetUid,
                        userDoc.getString("nickname"),
                        userDoc.getString("profileImageUrl")
                ));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
