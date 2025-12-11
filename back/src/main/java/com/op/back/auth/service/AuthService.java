package com.op.back.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;

import com.op.back.auth.dto.RegisterDTO;
import com.op.back.auth.dto.PetDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Blob;

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

        ref.set(data).get();

        return uid;
    }

    private String uploadFile(MultipartFile file, String path) throws Exception {
        if (file == null || file.isEmpty()) return null;

        Bucket bucket = StorageClient.getInstance(firebaseApp).bucket();
        Blob blob = bucket.create(path, file.getBytes(), file.getContentType());

        return "https://storage.googleapis.com/" + bucket.getName() + "/" + blob.getName();
    }
}
