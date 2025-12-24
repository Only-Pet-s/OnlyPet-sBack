package com.op.back.auth.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;

import com.op.back.auth.dto.LoginDTO;
import com.op.back.auth.dto.RegisterDTO;
import com.op.back.auth.dto.PetDTO;

import com.op.back.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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

    private String uploadFile(MultipartFile file, String path) throws Exception {
        if (file == null || file.isEmpty()) return null;

        Bucket bucket = StorageClient.getInstance(firebaseApp).bucket();
        Blob blob = bucket.create(path, file.getBytes(), file.getContentType());

        return "https://storage.googleapis.com/" + bucket.getName() + "/" + blob.getName();
    }

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
        refreshTokenService. saveRefreshToken(uid, rToken, refreshTTL);

        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("accessToken", token);
        result.put("refreshToken", rToken);
        result.put("userInfo", userInfo);

        return result;
    }

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

    public void logout(String uid){
        refreshTokenService.deleteRefreshToken(uid);
    }

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

        ref.delete().get();

        firebaseAuth.deleteUser(uid);

        refreshTokenService.deleteRefreshToken(uid);
    }
}
