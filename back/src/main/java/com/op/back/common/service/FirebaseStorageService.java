package com.op.back.common.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import com.google.cloud.storage.Bucket;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    //Storage파일 업로드 후, public URL 반환
    public String uploadFile(MultipartFile file, String path) throws IOException {

        Bucket bucket = StorageClient.getInstance().bucket();
        String bucketName = bucket.getName();

        String token = UUID.randomUUID().toString();

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, path)
                .setContentType(file.getContentType())
                .setMetadata(Map.of(
                        "firebaseStorageDownloadTokens", token
                ))
                .build();

        Blob blob = bucket.getStorage().create(blobInfo, file.getBytes());

        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
                bucketName,
                URLEncoder.encode(path, StandardCharsets.UTF_8),
                token
        );
    }

    //스토리지 삭제
    public void deleteFile(String mediaUrl) {
        try {
            if (mediaUrl == null || mediaUrl.isEmpty()) return;

            String bucketName = StorageClient.getInstance().bucket().getName();
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";

            if (!mediaUrl.startsWith(prefix)) return;

            String filePath = mediaUrl.substring(prefix.length());

            Bucket bucket = StorageClient.getInstance().bucket();
            boolean deleted = bucket.get(filePath).delete();

            System.out.println("Storage file delete: " + deleted + " (" + filePath + ")");

        } catch (Exception e) {
            System.out.println("Storage delete error: " + e.getMessage());
        }
    }
}
