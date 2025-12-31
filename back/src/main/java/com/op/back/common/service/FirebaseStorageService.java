package com.op.back.common.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;

import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FirebaseStorageService {

    //Storage파일 업로드 후, public URL 반환
    public String uploadFile(MultipartFile file, String path) throws IOException {

        Bucket bucket = StorageClient.getInstance().bucket();
        String bucketName = bucket.getName();

        Blob blob = bucket.create(
                path,
                file.getInputStream(),
                file.getContentType()
        );

        // 브라우저에서 바로 열리는 Firebase download URL
        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
                bucket.getName(),
                URLEncoder.encode(path, StandardCharsets.UTF_8),
                blob.getMetadata().get("firebaseStorageDownloadTokens")
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
