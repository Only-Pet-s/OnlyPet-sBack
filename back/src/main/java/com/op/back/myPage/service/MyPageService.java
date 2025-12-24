package com.op.back.myPage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.op.back.myPage.dto.MyPageDTO;
import com.op.back.myPage.dto.MyPagePostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void updateRole(
            String uid,
            Boolean seller,
            Boolean instructor,
            Boolean petsitter,
            MultipartFile businessFile,
            MultipartFile certificateFile
    ) throws Exception {

        DocumentReference ref = firestore.collection("users").document(uid);
        DocumentSnapshot snapshot = ref.get().get();

        boolean currentSeller = snapshot.getBoolean("seller") != null && snapshot.getBoolean("seller");
        boolean currentInstructor = snapshot.getBoolean("instructor") != null && snapshot.getBoolean("instructor");
        boolean currentPetsitter = snapshot.getBoolean("petsitter") != null && snapshot.getBoolean("petsitter");

        Map<String, Object> updates = new HashMap<>();

        // SELLER 처리
        if (seller != null && seller) {

            if (!currentSeller) {
                // false → true 로 전환 → 파일 필수
                if (businessFile == null) throw new RuntimeException("사업자 등록증 파일이 필요합니다.");
            }

            if (businessFile != null) {
                // 기존 파일 삭제
                String oldBiz = snapshot.getString("businessCertificateUrl");
                deleteFromStorage(oldBiz);

                // 새 파일 업로드
                String newBizUrl = uploadToStorage(uid, businessFile, "certificates");
                updates.put("businessCertificateUrl", newBizUrl);
            }

            updates.put("seller", seller);
        }

        // INSTRUCTOR 처리
        if (instructor != null && instructor) {

            if (!currentInstructor) {
                if (certificateFile == null) throw new RuntimeException("강의자 자격증 파일이 필요합니다.");
            }

            if (certificateFile != null) {
                String oldInst = snapshot.getString("instructorCertificateUrl");
                deleteFromStorage(oldInst);

                String newUrl = uploadToStorage(uid, certificateFile, "certificates");
                updates.put("instructorCertificateUrl", newUrl);
            }

            updates.put("instructor", instructor);
        }

        // PETSITTER 처리
        if (petsitter != null && petsitter) {

            if (!currentPetsitter) {
                if (certificateFile == null) throw new RuntimeException("펫시터 자격증 파일이 필요합니다.");
            }

            if (certificateFile != null) {
                String oldPet = snapshot.getString("petsitterCertificateUrl");
                deleteFromStorage(oldPet);

                String newUrl = uploadToStorage(uid, certificateFile, "certificates");
                updates.put("petsitterCertificateUrl", newUrl);
            }

            updates.put("petsitter", petsitter);
        }

        // 마지막으로 DB 업데이트
        if (!updates.isEmpty()) {
            ref.update(updates).get();
        }
    }

    public void updatePageVisible(String uid, String pageVisible) {
        validateVisible(pageVisible);

        firestore.collection("users").document(uid).update("pageVisible", pageVisible);
    }

    public void updateUserInfo(String uid, Map<String, Object> data) throws Exception{
        DocumentReference userRef = firestore.collection("users").document(uid);

        ApiFuture<WriteResult> future = userRef.update(data);
        future.get();
    }
    public String updateProfileImage(String uid, MultipartFile file) throws Exception{
        DocumentReference ref = firestore.collection("users").document(uid);
        DocumentSnapshot snapshot = ref.get().get();

        // 1) 기존 이미지 URL 가져오기
        String oldImageUrl = snapshot.contains("profileImageUrl") ?
                snapshot.getString("profileImageUrl") : null;

        // 2) 기존 파일 삭제
        deleteFromStorage(oldImageUrl);

        // 3) 새 파일 업로드
        String newUrl = uploadToStorage(uid, file, "profiles");

        // 4) Firestore 업데이트
        ref.update("profileImageUrl", newUrl);

        return newUrl;
    }

    public void updateCaption(String uid, Map<String, String> data) throws Exception {

        DocumentReference ref = firestore.collection("users").document(uid);

        Map<String, Object> updates = new HashMap<>();

        if (data.containsKey("captionTitle")) {
            String title = data.get("captionTitle");
            updates.put("captionTitle", title != null ? title : "");
        }

        if (data.containsKey("captionContent")) {
            String content = data.get("captionContent");
            updates.put("captionContent", content != null ? content : "");
        }

        if (updates.isEmpty()) {
            throw new RuntimeException("수정할 캡션 내용이 없습니다.");
        }

        ref.update(updates).get();
    }

    private void validateVisible(String pageVisible) {
        if (!"PUBLIC".equals(pageVisible)
                && !"FOLLOWER".equals(pageVisible)
                && !"PRIVATE".equals(pageVisible)) {
            throw new IllegalArgumentException("옳지 않은 마이 페이지 공개 상태");
        }
    }

    // firebase storage 업로드 메소드
    private String uploadToStorage(String uid, MultipartFile file, String type) throws Exception {
        String filename = type + "/" + uid + "_" + System.currentTimeMillis()
                + "_" + file.getOriginalFilename();

        Bucket bucket = StorageClient.getInstance().bucket();
        bucket.create(filename, file.getBytes(), file.getContentType());

        return "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName()
                + "/o/" + URLEncoder.encode(filename, "UTF-8") + "?alt=media";
    }

    // firebase storage에 기존에 존재하는 파일 삭제
    private void deleteFromStorage(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) return;

            // 1. 현재 버킷명 정확히 가져오기
            Bucket bucket = StorageClient.getInstance().bucket();
            String bucketName = bucket.getName();

            // 2. URL 디코딩
            String decoded = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);

            // 3. URL prefix, suffix 정의
            String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/";

            if (!decoded.startsWith(prefix)) {
                System.out.println("URL prefix not matching: " + decoded);
                return;
            }

            // 4. suffix 위치 찾기
            int endIndex = decoded.indexOf("?alt=");
            if (endIndex == -1) {
                System.out.println("URL missing ?alt= parameter");
                return;
            }

            // 5. object name 추출
            String objectName = decoded.substring(prefix.length(), endIndex);

            // 6. 파일 삭제
            boolean deleted = bucket.get(objectName).delete();

            //System.out.println("Deleted " + objectName + " = " + deleted);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Storage 파일 삭제 실패: " + e.getMessage());
        }
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
}

