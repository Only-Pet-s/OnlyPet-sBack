package com.op.back.auth.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;

import com.op.back.auth.dto.LoginDTO;
import com.op.back.auth.dto.RegisterDTO;

import com.op.back.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Blob;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class AuthService {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private Firestore firestore;

    @Autowired
    private FirebaseApp firebaseApp;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${firebase.api.key}")
    private String firebaseKey;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // 회원가입
    public String registerUser(RegisterDTO dto,
                               MultipartFile profileImage,
                               MultipartFile certificateFile) throws Exception {

        // 1) Firebase Auth 계정 생성
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(dto.getEmail())
                .setPassword(dto.getPassword())
                .setDisplayName(dto.getNickname());

        UserRecord userRecord = firebaseAuth.createUser(request);
        String uid = userRecord.getUid();

        // 2) Storage 업로드
        String profileUrl = uploadFile(profileImage, "profiles/" + uid + ".jpg");
        String certificateUrl = uploadFile(certificateFile, "certificates/" + uid + ".pdf");

        // 3) Firestore 저장
        DocumentReference ref = firestore.collection("users").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("name", dto.getName());
        data.put("nickname", dto.getNickname());
        data.put("email", dto.getEmail());
        data.put("address", dto.getAddress());
        data.put("phone", dto.getPhone());

        data.put("seller", dto.isSeller());
        data.put("instructor", dto.isInstructor());
        data.put("petsitter", dto.isPetsitter());
        data.put("businessNumber", dto.getBusinessNumber());

        data.put("profileImageUrl", profileUrl);
        data.put("certificateUrl", certificateUrl);

        data.put("animals", dto.getAnimals());

        data.put("followerCount", 0);
        data.put("followingCount", 0);
        data.put("postCount", 0);

        data.put("captionTitle", dto.getCaptionTitle() != null ? dto.getCaptionTitle() : "");
        data.put("captionContent", dto.getCaptionContent() != null ? dto.getCaptionContent() : "");

        data.put("pageVisible", dto.getPageVisible() != null ? dto.getPageVisible() : "PUBLIC"); // PUBLIC, FOLLOWER, PRIVATE

        ref.set(data).get();

        return uid;
    }

    // 로그인
    public Map<String, Object> login(LoginDTO dto) throws Exception {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseKey;

        RestTemplate rest = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("email", dto.getEmail());
        request.put("password", dto.getPassword());
        request.put("returnSecureToken", true);

        ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);

        if(!response.getStatusCode().is2xxSuccessful()){
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        String uid = (String) response.getBody().get("localId");

        DocumentSnapshot snapshot = firestore.collection("users").document(uid).get().get();

        if(!snapshot.exists()){
            throw new RuntimeException("사용자 정보가 일치하지 않습니다.");
        }

        Map<String, Object> userInfo = snapshot.getData();

        String token = jwtUtil.createToken(uid,dto.getEmail()); // AT

        String rToken = jwtUtil.createRefreshToken(uid); // RT

        long refreshTTL = 1000L * 60 * 60 * 24 * 30;
        refreshTokenService.saveRefreshToken(uid, rToken, refreshTTL);

        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("accessToken", token);
        result.put("refreshToken", rToken);
        result.put("userInfo", userInfo);

        return result;
    }

    // 리프레시(액세스 토큰 재발급)
    public Map<String, Object> refresh(String uid, String cRefreshToken){
        String storedToken = refreshTokenService.getRefreshToken(uid);

        if(storedToken == null){
            throw new RuntimeException("Refresh Token 존재하지 않습니다. 다시 로그인해주세요.");
        }

        if(!storedToken.equals(cRefreshToken)){
            throw new RuntimeException("Refresh Token 불일치");
        }

        if(!jwtUtil.validateToken(cRefreshToken)){
            throw new RuntimeException("Refresh Token이 만료되었습니다. 다시 로그인 해주세요.");
        }

        String email = jwtUtil.getEmail(cRefreshToken);
        String newAccessToken = jwtUtil.createToken(uid, email);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);

        return result;
    }

    // 로그아웃(리프레시 토큰 제거)
    public void logout(String uid){
        refreshTokenService.deleteRefreshToken(uid);
    }

    // 비밀번호 변경
    public void changePassword(String uid, String email, String oldPw, String newPw){

        // 먼저 이전 비밀번호 검증 절차
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseKey;

        RestTemplate rest = new RestTemplate();

        Map<String, Object> request1 = new HashMap<>();
        request1.put("email", email);
        request1.put("password", oldPw);
        request1.put("returnSecureToken", true);

        ResponseEntity<Map> response1 = rest.postForEntity(url, request1, Map.class);
        String idToken = (String) response1.getBody().get("idToken");

        if(!response1.getStatusCode().is2xxSuccessful()){
            throw new RuntimeException("기존 비밀번호가 일치하지 않습니다.");
        }

        // 그 이후 비밀번호 변경 절차
        String updateUrl = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + firebaseKey;

        Map<String, Object> request2 = new HashMap<>();
        request2.put("idToken", idToken);
        request2.put("password", newPw);
        request2.put("returnSecureToken", true);

        ResponseEntity<Map> updateRes =  rest.postForEntity(updateUrl, request2, Map.class);

        if(!updateRes.getStatusCode().is2xxSuccessful()){
            throw new RuntimeException("비밀번호 변경 실패");
        }
    }

    // 회원 탈퇴
    public void deleteUser(String uid) throws Exception{

        DocumentReference ref = firestore.collection("users").document(uid);
        DocumentSnapshot snapshot = ref.get().get();

        if(!snapshot.exists()){
            throw new RuntimeException("사용자 정보가 존재하지 않습니다.");
        }

        String pUrl = snapshot.getString("profileImageUrl");
        deleteFromStorage(pUrl);

        String cUrl = snapshot.getString("certificateUrl");
        deleteFromStorage(cUrl);
        String bcUrl = snapshot.getString("businessCertificateUrl");
        deleteFromStorage(bcUrl);
        String icUrl = snapshot.getString("instructorCertificateUrl");
        deleteFromStorage(icUrl);
        String pcUrl = snapshot.getString("petsitterCertificateUrl");
        deleteFromStorage(pcUrl);

        // 내가 팔로잉한 사람의 followers에서 나 삭제
        CollectionReference myFollowing = ref.collection("following");
        List<QueryDocumentSnapshot> followingDocs = myFollowing.get().get().getDocuments();

        for (QueryDocumentSnapshot doc : followingDocs) {
            String targetUid = doc.getId();  // 내가 팔로잉한 사람 uid
            firestore.collection("users")
                    .document(targetUid)
                    .collection("followers")
                    .document(uid)
                    .delete();
        }

        // 나를 팔로우하는 사람의 following에서 나 삭제
        CollectionReference myFollowers = ref.collection("followers");
        List<QueryDocumentSnapshot> followerDocs = myFollowers.get().get().getDocuments();

        for (QueryDocumentSnapshot doc : followerDocs) {
            String targetUid = doc.getId();  // 나를 팔로우한 사람 uid
            firestore.collection("users")
                    .document(targetUid)
                    .collection("following")
                    .document(uid)
                    .delete();
        }

        // 내 followers/following subcollection 삭제
        for (QueryDocumentSnapshot doc : followingDocs) {
            myFollowing.document(doc.getId()).delete();
        }
        for (QueryDocumentSnapshot doc : followerDocs) {
            myFollowers.document(doc.getId()).delete();
        }

        ref.delete().get();

        firebaseAuth.deleteUser(uid);

        refreshTokenService.deleteRefreshToken(uid);
    }

    public boolean pwValidation(String email, String pw) {
        // 먼저 이전 비밀번호 검증 절차
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseKey;

        RestTemplate rest = new RestTemplate();
        Map<String, Object> request1 = new HashMap<>();
        request1.put("email", email);
        request1.put("password", pw);
        request1.put("returnSecureToken", true);

        ResponseEntity<Map> response1 = rest.postForEntity(url, request1, Map.class);

        if(response1.getStatusCode().is2xxSuccessful()){
            return true;
        }
        return false;
    }

    private String uploadFile(MultipartFile file, String path) throws Exception {
        if (file == null || file.isEmpty()) return null;

        Bucket bucket = StorageClient.getInstance(firebaseApp).bucket();
        Blob blob = bucket.create(path, file.getBytes(), file.getContentType());

        return "https://storage.googleapis.com/" + bucket.getName() + "/" + blob.getName();
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
}
