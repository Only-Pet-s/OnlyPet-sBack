package com.op.back.myPage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.op.back.myPage.dto.MyPageDTO;
import com.op.back.myPage.dto.MyPagePostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final Firestore firestore;

    // 프로필 조회
    public MyPageDTO getProfile(String targetUid, String loginUid)
            throws Exception {

        DocumentSnapshot userDoc =
                firestore.collection("users").document(targetUid).get().get();

        if (!userDoc.exists()) {
            throw new RuntimeException("사용자가 존재하지 않습니다.");
        }

        String visible = userDoc.getString("pageVisible");

        boolean isOwner = targetUid.equals(loginUid);

        if(!openMyPage(visible, targetUid, loginUid, isOwner)){
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }

        return new MyPageDTO(
                userDoc.getString("nickname"),
                userDoc.getString("profileImageUrl"),
                userDoc.getString("captionTitle"),
                userDoc.getString("captionContent"),
                userDoc.getLong("postCount"),
                userDoc.getLong("followerCount"),
                userDoc.getLong("followingCount"),
                isOwner
        );
    }

    // 게시물 썸네일
    public List<MyPagePostDTO> getMyPosts(String uid)
            throws Exception {

        QuerySnapshot snapshot = firestore.collection("posts")
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(12)
                .get()
                .get();

        return snapshot.getDocuments()
                .stream()
                .map(MyPagePostDTO::from)
                .toList();
    }

    private boolean openMyPage(
            String visible, String targetUid, String loginUid, boolean isOwner
    ) {
        if (isOwner) {
            return true;
        }
        if("PUBLIC".equals(visible)){
            return true;
        }
        if("PRIVATE".equals(visible)){
            return false;
        }
        if("FOLLOWER".equals(visible)){
            if(loginUid == null) return false;
            return isFollower(loginUid, targetUid); // 수정 필요
        }
        return false;
    }

    private boolean isFollower(String loginUid, String targetUid) { // 임시 메소드, 실제 팔로잉/팔로워 관리 필요함
        try {
            DocumentSnapshot doc = firestore
                    .collection("follows")
                    .document(targetUid)
                    .collection("followers")
                    .document(loginUid)
                    .get()
                    .get();

            return doc.exists();
        } catch (Exception e) {
            return false;
        }
    }

    public void updatePageVisible(String uid, String pageVisible) {
        validateVisible(pageVisible);

        firestore.collection("users").document(uid).update("pageVisible", pageVisible);
    }

    private void validateVisible(String pageVisible) {
        if (!"PUBLIC".equals(pageVisible)
                && !"FOLLOWER".equals(pageVisible)
                && !"PRIVATE".equals(pageVisible)) {
            throw new IllegalArgumentException("옳지 않은 마이 페이지 공개 상태");
        }
    }
}

