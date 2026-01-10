package com.op.back.common.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;

import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.IOException;
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

    //MultipartFile없이 썸네일 업로드 가능하도록 확장
    public String uploadBytes(byte[] bytes, String contentType, String path) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket();
        String bucketName = bucket.getName();

        String token = UUID.randomUUID().toString();

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, path)
                .setContentType(contentType)
                .setMetadata(Map.of(
                        "firebaseStorageDownloadTokens", token
                ))
                .build();

        bucket.getStorage().create(blobInfo, bytes);

        return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
                bucketName,
                URLEncoder.encode(path, StandardCharsets.UTF_8),
                token
        );
    }

    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            int start = fileUrl.indexOf("/o/") + 3;
            int end = fileUrl.indexOf("?");

            if (start < 3 || end < 0) {
                System.out.println("Invalid Firebase Storage URL: " + fileUrl);
                return;
            }

            String encodedPath = fileUrl.substring(start, end);
            String decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);

            Bucket bucket = StorageClient.getInstance().bucket();
            Blob blob = bucket.get(decodedPath);

            if (blob == null) {
                System.out.println("Storage file not found: " + decodedPath);
                return;
            }

            boolean deleted = blob.delete();
            System.out.println("Storage delete result: " + deleted + " (" + decodedPath + ")");

        } catch (Exception e) {
            System.out.println("Storage delete error: " + e.getMessage());
        }
    }

}
